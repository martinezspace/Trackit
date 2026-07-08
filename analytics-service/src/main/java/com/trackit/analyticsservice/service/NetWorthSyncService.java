package com.trackit.analyticsservice.service;

import com.trackit.analyticsservice.client.BankAccountServiceClient;
import com.trackit.analyticsservice.client.InvestmentServiceClient;
import com.trackit.analyticsservice.dto.client.bank.BankAccountDTO;
import com.trackit.analyticsservice.dto.client.investment.InvestmentAccountDTO;
import com.trackit.analyticsservice.dto.client.investment.PortfolioSnapshotDTO;
import com.trackit.analyticsservice.dto.response.NetWorthSnapshotResponseDTO;
import com.trackit.analyticsservice.mapper.NetWorthSnapshotMapper;
import com.trackit.analyticsservice.model.NetWorthSnapshot;
import com.trackit.analyticsservice.repository.NetWorthSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetWorthSyncService {

    private final NetWorthSnapshotRepository repository;
    private final NetWorthSnapshotMapper mapper;
    private final InvestmentServiceClient investmentClient;
    private final BankAccountServiceClient bankAccountClient;

    // --- Sync ---

    public void sync(UUID userId) {
        log.info("Starting net worth sync for user {}", userId);

        BigDecimal bankTotal = sumBankBalances(userId);
        BigDecimal investmentTotal = sumInvestmentValues(userId);
        String currency = resolveCurrency(userId);

        LocalDate today = LocalDate.now();
        Optional<NetWorthSnapshot> existing = repository.findByUserIdAndSnapshotDate(userId, today);

        NetWorthSnapshot snapshot = existing.orElseGet(NetWorthSnapshot::new);
        snapshot.setUserId(userId);
        snapshot.setSnapshotDate(today);
        snapshot.setBankBalanceTotal(bankTotal);
        snapshot.setInvestmentValueTotal(investmentTotal);
        snapshot.setNetWorth(bankTotal.add(investmentTotal));
        snapshot.setCurrency(currency);

        repository.save(snapshot);
        log.info("Net worth sync complete for user {} — net worth: {} {}", userId, snapshot.getNetWorth(), currency);
    }

    // --- Queries ---

    public List<NetWorthSnapshotResponseDTO> getAll(UUID userId) {
        return repository.findByUserIdOrderBySnapshotDateAsc(userId)
                .stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    public List<NetWorthSnapshotResponseDTO> getRange(UUID userId, LocalDate from, LocalDate to) {
        return repository.findByUserIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(userId, from, to)
                .stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    public NetWorthSnapshotResponseDTO getLatest(UUID userId) {
        return repository.findTopByUserIdOrderBySnapshotDateDesc(userId)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("No net worth snapshots found for user " + userId));
    }

    // --- Helpers ---

    private BigDecimal sumBankBalances(UUID userId) {
        try {
            List<BankAccountDTO> accounts = bankAccountClient.getBankAccounts(userId);
            return accounts.stream()
                    .filter(BankAccountDTO::isActive)
                    // Skip accounts where balance hasn't been fetched from Tink yet
                    .filter(a -> a.getCurrentBalance() != null)
                    .map(a -> new BigDecimal(a.getCurrentBalance()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.error("Failed to fetch bank balances for user {} — defaulting to 0", userId, e);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal sumInvestmentValues(UUID userId) {
        try {
            List<InvestmentAccountDTO> accounts = investmentClient.getInvestmentAccounts(userId);
            return accounts.stream()
                    .filter(InvestmentAccountDTO::isActive)
                    .map(account -> {
                        PortfolioSnapshotDTO snapshot = investmentClient
                                .getLatestPortfolioSnapshot(UUID.fromString(account.getId()), userId);
                        if (snapshot == null || snapshot.getTotalValue() == null) return BigDecimal.ZERO;
                        return new BigDecimal(snapshot.getTotalValue());
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.error("Failed to fetch investment values for user {} — defaulting to 0", userId, e);
            return BigDecimal.ZERO;
        }
    }

    // Simplification — uses currency from the first active bank account
    // Multi-currency support requires FX conversion and is deferred
    private String resolveCurrency(UUID userId) {
        try {
            return bankAccountClient.getBankAccounts(userId).stream()
                    .filter(BankAccountDTO::isActive)
                    .map(BankAccountDTO::getCurrency)
                    .findFirst()
                    .orElse("EUR");
        } catch (Exception e) {
            return "EUR";
        }
    }
}