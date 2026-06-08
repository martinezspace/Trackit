package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.client.TinkClient;
import com.trackit.bankaccountservice.dto.BankAccountCreateDTO;
import com.trackit.bankaccountservice.dto.BankAccountResponseDTO;
import com.trackit.bankaccountservice.dto.SyncLogResponseDTO;
import com.trackit.bankaccountservice.dto.TransactionCreateDTO;
import com.trackit.bankaccountservice.dto.tink.TinkAccountDTO;
import com.trackit.bankaccountservice.dto.tink.TinkGrantResponseDTO;
import com.trackit.bankaccountservice.dto.tink.TinkTransactionDTO;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.model.*;
import com.trackit.bankaccountservice.repository.BankConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TinkSyncService {

    //How far back to fetch on first sync when no lastSyncedAt exists
    private static final int DEFAULT_LOOKBACK_DAYS = 90;

    //Overlap ensures transactions that settled just after the last sync aren't missed
    private static final int SYNC_OVERLAP_DAYS = 1;

    private final BankConnectionRepository connectionRepository;
    private final BankAccountService bankAccountService;
    private final TransactionService transactionService;
    private final SyncLogService syncLogService;
    private final TinkClient tinkClient;
    private final TinkTokenService tokenService;

    public void syncConnection(UUID connectionId, UUID userId, SyncTrigger trigger) {
        SyncLogResponseDTO syncLog = syncLogService.startSync(connectionId, userId, trigger);
        UUID syncLogId = UUID.fromString(syncLog.getId());

        try {
            BankConnection connection = connectionRepository.findByIdAndUserId(connectionId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Connection not found: " + connectionId));

            //Delegation grant → short-lived user token, not stored
            TinkGrantResponseDTO grant = tinkClient.createAuthorizationGrant(connection.getTinkUserId());
            String userToken = tokenService.getUserAccessToken(grant.getCode());

            LocalDate from = connection.getLastSyncedAt() != null
                    ? connection.getLastSyncedAt().toLocalDate().minusDays(SYNC_OVERLAP_DAYS)
                    : LocalDate.now().minusDays(DEFAULT_LOOKBACK_DAYS);

            List<TinkAccountDTO> tinkAccounts = tinkClient.getAccounts(userToken);
            int totalFetched = 0;

            for (TinkAccountDTO tinkAccount : tinkAccounts) {
                BankAccountResponseDTO account = bankAccountService.createAccount(
                        userId, toAccountCreateDTO(tinkAccount, connectionId));

                updateBalances(tinkAccount, UUID.fromString(account.getId()), userId);

                List<TinkTransactionDTO> transactions = tinkClient.getAllTransactions(
                        userToken, tinkAccount.getId(), from);
                totalFetched += transactions.size();

                for (TinkTransactionDTO tx : transactions) {
                    transactionService.createTransaction(
                            userId, toTransactionCreateDTO(tx, UUID.fromString(account.getId())));
                }
            }

            syncLogService.completeSyncLog(syncLogId, totalFetched, totalFetched, 0);

        } catch (Exception e) {
            log.error("Sync failed for connection {}: {}", connectionId, e.getMessage());
            syncLogService.failSyncLog(syncLogId, e.getMessage());
        }
    }

    //Fixed delay so syncs don't overlap if one run takes longer than the interval
    @Scheduled(fixedDelayString = "${tink.sync-interval-hours}", timeUnit = TimeUnit.HOURS)
    public void syncAllActive() {
        connectionRepository.findByStatus(ConnectionStatus.ACTIVE)
                .forEach(conn -> {
                    try {
                        syncConnection(conn.getId(), conn.getUserId(), SyncTrigger.SCHEDULED);
                    } catch (Exception e) {
                        log.error("Scheduled sync failed for connection {}: {}", conn.getId(), e.getMessage());
                    }
                });
    }

    // --- Conversion helpers ---

    private BankAccountCreateDTO toAccountCreateDTO(TinkAccountDTO tink, UUID connectionId) {
        BankAccountCreateDTO dto = new BankAccountCreateDTO();
        dto.setConnectionId(connectionId);
        dto.setExternalAccountId(tink.getId());
        dto.setName(tink.getName());
        dto.setAccountType(mapAccountType(tink.getType()));

        if (tink.getIdentifiers() != null && tink.getIdentifiers().getIban() != null) {
            dto.setIban(tink.getIdentifiers().getIban().getIban());
        }

        if (tink.getBalance() != null
                && tink.getBalance().getBookedBalance() != null
                && tink.getBalance().getBookedBalance().getAmount() != null) {
            dto.setCurrency(tink.getBalance().getBookedBalance().getAmount().getCurrencyCode());
        }

        return dto;
    }

    private void updateBalances(TinkAccountDTO tink, UUID accountId, UUID userId) {
        if (tink.getBalance() == null) return;

        BigDecimal booked = tink.getBalance().getBookedBalance() != null
                && tink.getBalance().getBookedBalance().getAmount() != null
                ? tink.getBalance().getBookedBalance().getAmount().toBigDecimal()
                : BigDecimal.ZERO;

        BigDecimal available = tink.getBalance().getAvailableBalance() != null
                && tink.getBalance().getAvailableBalance().getAmount() != null
                ? tink.getBalance().getAvailableBalance().getAmount().toBigDecimal()
                : booked;

        bankAccountService.updateBalance(accountId, userId, booked, available);
    }

    private TransactionCreateDTO toTransactionCreateDTO(TinkTransactionDTO tink, UUID accountId) {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setAccountId(accountId);

        BigDecimal rawAmount = tink.getAmount() != null
                ? tink.getAmount().toBigDecimal()
                : BigDecimal.ZERO;

        //Tink uses signed amounts — negative = outbound, positive = inbound
        dto.setAmount(rawAmount.abs());
        dto.setDirection(rawAmount.signum() < 0 ? TransactionDirection.OUTBOUND : TransactionDirection.INBOUND);
        dto.setCurrency(tink.getAmount() != null ? tink.getAmount().getCurrencyCode() : "PLN");

        if (tink.getDates() != null) {
            dto.setBookingDate(LocalDate.parse(tink.getDates().getBooked()));
            if (tink.getDates().getValue() != null) {
                dto.setValueDate(LocalDate.parse(tink.getDates().getValue()));
            }
        }

        if (tink.getDescriptions() != null) {
            String display = tink.getDescriptions().getDisplay();
            dto.setDescription(display != null ? display : tink.getDescriptions().getOriginal());
        }

        if (tink.getMerchantInformation() != null) {
            dto.setMerchantName(tink.getMerchantInformation().getMerchantName());
        }

        //Prefer stable provider ID for deduplication; fall back to Tink's own transaction ID
        String externalId = tink.getIdentifiers() != null
                && tink.getIdentifiers().getProviderTransactionId() != null
                ? tink.getIdentifiers().getProviderTransactionId()
                : tink.getId();
        dto.setExternalId(externalId);

        return dto;
    }

    private AccountType mapAccountType(String tinkType) {
        if (tinkType == null) return AccountType.OTHER;
        return switch (tinkType) {
            case "CHECKING" -> AccountType.CURRENT;
            case "SAVINGS" -> AccountType.SAVINGS;
            case "CREDIT_CARD" -> AccountType.CREDIT;
            case "INVESTMENT" -> AccountType.INVESTMENT;
            default -> AccountType.OTHER;
        };
    }
}