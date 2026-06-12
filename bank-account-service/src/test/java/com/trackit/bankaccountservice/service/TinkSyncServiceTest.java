package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.dto.BankAccountCreateDTO;
import com.trackit.bankaccountservice.dto.BankAccountResponseDTO;
import com.trackit.bankaccountservice.dto.SyncLogResponseDTO;
import com.trackit.bankaccountservice.dto.TransactionCreateDTO;
import com.trackit.bankaccountservice.client.TinkClient;
import com.trackit.bankaccountservice.dto.tink.TinkAccountDTO;
import com.trackit.bankaccountservice.dto.tink.TinkGrantResponseDTO;
import com.trackit.bankaccountservice.dto.tink.TinkMoneyDTO;
import com.trackit.bankaccountservice.dto.tink.TinkTransactionDTO;
import com.trackit.bankaccountservice.model.*;
import com.trackit.bankaccountservice.repository.BankConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TinkSyncServiceTest {

    @Mock
    private BankConnectionRepository connectionRepository;

    @Mock
    private BankAccountService bankAccountService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private SyncLogService syncLogService;

    @Mock
    private TinkClient tinkClient;

    @Mock
    private TinkTokenService tokenService;

    @InjectMocks
    private TinkSyncService syncService;

    private UUID userId;
    private UUID connectionId;
    private UUID syncLogId;
    private BankConnection connection;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        connectionId = UUID.randomUUID();
        syncLogId = UUID.randomUUID();

        connection = new BankConnection();
        connection.setUserId(userId);
        connection.setTinkUserId("tink-user-123");

        // Shared happy-path stubs — individual tests override what they need
        SyncLogResponseDTO syncLog = new SyncLogResponseDTO();
        syncLog.setId(syncLogId.toString());
        when(syncLogService.startSync(connectionId, userId, SyncTrigger.MANUAL)).thenReturn(syncLog);
        when(connectionRepository.findByIdAndUserId(connectionId, userId)).thenReturn(Optional.of(connection));

        TinkGrantResponseDTO grant = new TinkGrantResponseDTO();
        grant.setCode("grant-code");
        when(tinkClient.createAuthorizationGrant("tink-user-123")).thenReturn(grant);
        when(tokenService.getUserAccessToken("grant-code")).thenReturn("user-token");

        BankAccountResponseDTO accountResponse = new BankAccountResponseDTO();
        accountResponse.setId(UUID.randomUUID().toString());
        when(bankAccountService.createAccount(eq(userId), any())).thenReturn(accountResponse);
    }

    // syncConnection

    @Test
    public void syncConnection_completesLog_withCorrectTransactionCount() {
        when(tinkClient.getAccounts("user-token")).thenReturn(List.of(buildAccount("acc-1", "CHECKING", "1000", "PLN")));
        when(tinkClient.getAllTransactions(eq("user-token"), eq("acc-1"), any()))
                .thenReturn(List.of(buildTransaction("tx-1", "-100", "PLN", "2024-01-10")));

        syncService.syncConnection(connectionId, userId, SyncTrigger.MANUAL);

        verify(bankAccountService).createAccount(eq(userId), any());
        verify(transactionService).createTransaction(eq(userId), any());
        verify(syncLogService).completeSyncLog(syncLogId, 1, 1, 0);
    }

    @Test
    public void syncConnection_failsLog_whenExceptionOccurs() {
        when(connectionRepository.findByIdAndUserId(connectionId, userId))
                .thenThrow(new RuntimeException("DB timeout"));

        syncService.syncConnection(connectionId, userId, SyncTrigger.MANUAL);

        verify(syncLogService).failSyncLog(syncLogId, "DB timeout");
        verify(syncLogService, never()).completeSyncLog(any(), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void syncConnection_usesDefaultLookback_whenLastSyncedAtIsNull() {
        setupSingleAccountSync(buildAccount("acc-1", "CHECKING", "0", "PLN"));

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(tinkClient).getAllTransactions(any(), any(), fromCaptor.capture());
        assertThat(fromCaptor.getValue()).isBetween(LocalDate.now().minusDays(91), LocalDate.now().minusDays(89));
    }

    @Test
    public void syncConnection_usesLastSyncedAtMinusOverlap_whenPresent() {
        LocalDateTime lastSynced = LocalDateTime.now().minusDays(5);
        connection.setLastSyncedAt(lastSynced);
        setupSingleAccountSync(buildAccount("acc-1", "CHECKING", "0", "PLN"));

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(tinkClient).getAllTransactions(any(), any(), fromCaptor.capture());
        assertThat(fromCaptor.getValue()).isEqualTo(lastSynced.toLocalDate().minusDays(1));
    }

    // mapAccountType

    @Test
    public void syncConnection_mapsCheckingAccount_toCURRENT() {
        setupSingleAccountSync(buildAccount("acc-1", "CHECKING", "0", "PLN"));
        assertAccountType(AccountType.CURRENT);
    }

    @Test
    public void syncConnection_mapsSavingsAccount_toSAVINGS() {
        setupSingleAccountSync(buildAccount("acc-1", "SAVINGS", "0", "PLN"));
        assertAccountType(AccountType.SAVINGS);
    }

    @Test
    public void syncConnection_mapsCreditCardAccount_toCREDIT() {
        setupSingleAccountSync(buildAccount("acc-1", "CREDIT_CARD", "0", "PLN"));
        assertAccountType(AccountType.CREDIT);
    }

    @Test
    public void syncConnection_mapsInvestmentAccount_toINVESTMENT() {
        setupSingleAccountSync(buildAccount("acc-1", "INVESTMENT", "0", "PLN"));
        assertAccountType(AccountType.INVESTMENT);
    }

    @Test
    public void syncConnection_mapsUnknownAccountType_toOTHER() {
        setupSingleAccountSync(buildAccount("acc-1", "LOAN", "0", "PLN"));
        assertAccountType(AccountType.OTHER);
    }

    @Test
    public void syncConnection_mapsNullAccountType_toOTHER() {
        setupSingleAccountSync(buildAccount("acc-1", null, "0", "PLN"));
        assertAccountType(AccountType.OTHER);
    }

    // toAccountCreateDTO

    @Test
    public void syncConnection_mapsAccountFields_fromTinkAccount() {
        TinkAccountDTO account = buildAccount("acc-ext-1", "CHECKING", "500", "PLN");
        account.setName("My Current Account");
        TinkAccountDTO.Identifiers.Iban iban = new TinkAccountDTO.Identifiers.Iban();
        iban.setIban("PL61109010140000071219812874");
        TinkAccountDTO.Identifiers identifiers = new TinkAccountDTO.Identifiers();
        identifiers.setIban(iban);
        account.setIdentifiers(identifiers);

        setupSingleAccountSync(account);

        ArgumentCaptor<BankAccountCreateDTO> captor = ArgumentCaptor.forClass(BankAccountCreateDTO.class);
        verify(bankAccountService).createAccount(any(), captor.capture());
        assertThat(captor.getValue().getExternalAccountId()).isEqualTo("acc-ext-1");
        assertThat(captor.getValue().getName()).isEqualTo("My Current Account");
        assertThat(captor.getValue().getConnectionId()).isEqualTo(connectionId);
        assertThat(captor.getValue().getIban()).isEqualTo("PL61109010140000071219812874");
        assertThat(captor.getValue().getCurrency()).isEqualTo("PLN");
    }

    @Test
    public void syncConnection_handlesNullIdentifiers_withoutNPE() {
        TinkAccountDTO account = buildAccount("acc-1", "CHECKING", "0", "PLN");
        account.setIdentifiers(null);
        setupSingleAccountSync(account);

        ArgumentCaptor<BankAccountCreateDTO> captor = ArgumentCaptor.forClass(BankAccountCreateDTO.class);
        verify(bankAccountService).createAccount(any(), captor.capture());
        assertThat(captor.getValue().getIban()).isNull();
    }

    @Test
    public void syncConnection_handlesNullBalance_withoutNPE() {
        TinkAccountDTO account = buildAccount("acc-1", "CHECKING", "0", "PLN");
        account.setBalance(null);
        setupSingleAccountSync(account);

        ArgumentCaptor<BankAccountCreateDTO> captor = ArgumentCaptor.forClass(BankAccountCreateDTO.class);
        verify(bankAccountService).createAccount(any(), captor.capture());
        assertThat(captor.getValue().getCurrency()).isNull();
    }

    // toTransactionCreateDTO

    @Test
    public void syncConnection_mapsNegativeAmount_toOUTBOUND_withAbsValue() {
        setupSingleTransactionSync(buildTransaction("tx-1", "-250", "PLN", "2024-03-01"));

        ArgumentCaptor<TransactionCreateDTO> captor = ArgumentCaptor.forClass(TransactionCreateDTO.class);
        verify(transactionService).createTransaction(any(), captor.capture());
        assertThat(captor.getValue().getDirection()).isEqualTo(TransactionDirection.OUTBOUND);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("250");
    }

    @Test
    public void syncConnection_mapsPositiveAmount_toINBOUND() {
        setupSingleTransactionSync(buildTransaction("tx-1", "1500", "PLN", "2024-03-01"));

        ArgumentCaptor<TransactionCreateDTO> captor = ArgumentCaptor.forClass(TransactionCreateDTO.class);
        verify(transactionService).createTransaction(any(), captor.capture());
        assertThat(captor.getValue().getDirection()).isEqualTo(TransactionDirection.INBOUND);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("1500");
    }

    @Test
    public void syncConnection_prefersDisplayDescription_overOriginal() {
        TinkTransactionDTO tx = buildTransaction("tx-1", "-50", "PLN", "2024-03-01");
        TinkTransactionDTO.Descriptions desc = new TinkTransactionDTO.Descriptions();
        desc.setDisplay("Coffee Shop");
        desc.setOriginal("RAW MERCHANT STRING");
        tx.setDescriptions(desc);
        setupSingleTransactionSync(tx);

        ArgumentCaptor<TransactionCreateDTO> captor = ArgumentCaptor.forClass(TransactionCreateDTO.class);
        verify(transactionService).createTransaction(any(), captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("Coffee Shop");
    }

    @Test
    public void syncConnection_fallsBackToOriginalDescription_whenDisplayNull() {
        TinkTransactionDTO tx = buildTransaction("tx-1", "-50", "PLN", "2024-03-01");
        TinkTransactionDTO.Descriptions desc = new TinkTransactionDTO.Descriptions();
        desc.setDisplay(null);
        desc.setOriginal("RAW MERCHANT STRING");
        tx.setDescriptions(desc);
        setupSingleTransactionSync(tx);

        ArgumentCaptor<TransactionCreateDTO> captor = ArgumentCaptor.forClass(TransactionCreateDTO.class);
        verify(transactionService).createTransaction(any(), captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("RAW MERCHANT STRING");
    }

    @Test
    public void syncConnection_usesProviderTransactionId_asExternalId() {
        TinkTransactionDTO tx = buildTransaction("tink-tx-1", "-50", "PLN", "2024-03-01");
        TinkTransactionDTO.Identifiers ids = new TinkTransactionDTO.Identifiers();
        ids.setProviderTransactionId("provider-stable-id");
        tx.setIdentifiers(ids);
        setupSingleTransactionSync(tx);

        ArgumentCaptor<TransactionCreateDTO> captor = ArgumentCaptor.forClass(TransactionCreateDTO.class);
        verify(transactionService).createTransaction(any(), captor.capture());
        assertThat(captor.getValue().getExternalId()).isEqualTo("provider-stable-id");
    }

    @Test
    public void syncConnection_fallsBackToTinkId_whenNoProviderTransactionId() {
        TinkTransactionDTO tx = buildTransaction("tink-tx-1", "-50", "PLN", "2024-03-01");
        tx.setIdentifiers(null);
        setupSingleTransactionSync(tx);

        ArgumentCaptor<TransactionCreateDTO> captor = ArgumentCaptor.forClass(TransactionCreateDTO.class);
        verify(transactionService).createTransaction(any(), captor.capture());
        assertThat(captor.getValue().getExternalId()).isEqualTo("tink-tx-1");
    }

    @Test
    public void syncConnection_setsMerchantName_fromMerchantInformation() {
        TinkTransactionDTO tx = buildTransaction("tx-1", "-30", "PLN", "2024-03-01");
        TinkTransactionDTO.MerchantInformation merchant = new TinkTransactionDTO.MerchantInformation();
        merchant.setMerchantName("Starbucks");
        tx.setMerchantInformation(merchant);
        setupSingleTransactionSync(tx);

        ArgumentCaptor<TransactionCreateDTO> captor = ArgumentCaptor.forClass(TransactionCreateDTO.class);
        verify(transactionService).createTransaction(any(), captor.capture());
        assertThat(captor.getValue().getMerchantName()).isEqualTo("Starbucks");
    }

    @Test
    public void syncConnection_setsBookingDate_andValueDate() {
        TinkTransactionDTO tx = buildTransaction("tx-1", "-30", "PLN", "2024-03-15");
        tx.getDates().setValue("2024-03-17");
        setupSingleTransactionSync(tx);

        ArgumentCaptor<TransactionCreateDTO> captor = ArgumentCaptor.forClass(TransactionCreateDTO.class);
        verify(transactionService).createTransaction(any(), captor.capture());
        assertThat(captor.getValue().getBookingDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(captor.getValue().getValueDate()).isEqualTo(LocalDate.of(2024, 3, 17));
    }

    // Helpers

    private void setupSingleAccountSync(TinkAccountDTO account) {
        when(tinkClient.getAccounts("user-token")).thenReturn(List.of(account));
        when(tinkClient.getAllTransactions(eq("user-token"), eq(account.getId()), any())).thenReturn(List.of());
        syncService.syncConnection(connectionId, userId, SyncTrigger.MANUAL);
    }

    private void setupSingleTransactionSync(TinkTransactionDTO tx) {
        TinkAccountDTO account = buildAccount("acc-1", "CHECKING", "0", "PLN");
        when(tinkClient.getAccounts("user-token")).thenReturn(List.of(account));
        when(tinkClient.getAllTransactions(eq("user-token"), eq("acc-1"), any())).thenReturn(List.of(tx));
        syncService.syncConnection(connectionId, userId, SyncTrigger.MANUAL);
    }

    private void assertAccountType(AccountType expected) {
        ArgumentCaptor<BankAccountCreateDTO> captor = ArgumentCaptor.forClass(BankAccountCreateDTO.class);
        verify(bankAccountService).createAccount(any(), captor.capture());
        assertThat(captor.getValue().getAccountType()).isEqualTo(expected);
    }

    private TinkAccountDTO buildAccount(String id, String type, String unscaledValue, String currency) {
        TinkAccountDTO.Balance.BalanceEntry bookedEntry = new TinkAccountDTO.Balance.BalanceEntry();
        bookedEntry.setAmount(buildMoney(unscaledValue, currency));

        TinkAccountDTO.Balance balance = new TinkAccountDTO.Balance();
        balance.setBookedBalance(bookedEntry);
        balance.setAvailableBalance(bookedEntry);

        TinkAccountDTO account = new TinkAccountDTO();
        account.setId(id);
        account.setName("Test Account");
        account.setType(type);
        account.setBalance(balance);
        return account;
    }

    private TinkTransactionDTO buildTransaction(String id, String unscaledValue, String currency, String bookedDate) {
        TinkTransactionDTO.Dates dates = new TinkTransactionDTO.Dates();
        dates.setBooked(bookedDate);

        TinkTransactionDTO tx = new TinkTransactionDTO();
        tx.setId(id);
        tx.setAmount(buildMoney(unscaledValue, currency));
        tx.setDates(dates);
        return tx;
    }

    private TinkMoneyDTO buildMoney(String unscaledValue, String currency) {
        TinkMoneyDTO.Value value = new TinkMoneyDTO.Value();
        value.setUnscaledValue(unscaledValue);
        value.setScale(0);

        TinkMoneyDTO money = new TinkMoneyDTO();
        money.setCurrencyCode(currency);
        money.setValue(value);
        return money;
    }
}