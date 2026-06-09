package com.trackit.bankaccountservice.repository;

import com.trackit.bankaccountservice.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private BankConnectionRepository bankConnectionRepository;

    private UUID userId;
    private UUID otherUserId;
    private BankAccount account;
    private Transaction inboundLarge;
    private Transaction outboundSmall;
    private Transaction outboundManual;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        BankConnection conn = new BankConnection();
        conn.setUserId(userId);
        conn.setInstitutionId("MONZO_GB");
        conn.setInstitutionName("Monzo");
        conn.setCredentialsId("req-monzo-001");
        conn.setStatus(ConnectionStatus.ACTIVE);
        bankConnectionRepository.save(conn);

        account = new BankAccount();
        account.setConnection(conn);
        account.setUserId(userId);
        account.setExternalAccountId("ext-001");
        account.setName("Current Account");
        account.setCurrency("PLN");
        account.setActive(true);
        bankAccountRepository.save(account);

        inboundLarge = new Transaction();
        inboundLarge.setAccount(account);
        inboundLarge.setUserId(userId);
        inboundLarge.setExternalId("ext-tx-001");
        inboundLarge.setAmount(new BigDecimal("5000.00"));
        inboundLarge.setDirection(TransactionDirection.INBOUND);
        inboundLarge.setCurrency("PLN");
        inboundLarge.setBookingDate(LocalDate.now());
        inboundLarge.setCategorizationStatus(CategorizationStatus.UNCATEGORIZED);
        transactionRepository.save(inboundLarge);

        outboundSmall = new Transaction();
        outboundSmall.setAccount(account);
        outboundSmall.setUserId(userId);
        outboundSmall.setExternalId("ext-tx-002");
        outboundSmall.setAmount(new BigDecimal("29.99"));
        outboundSmall.setDirection(TransactionDirection.OUTBOUND);
        outboundSmall.setCurrency("PLN");
        outboundSmall.setBookingDate(LocalDate.now().minusDays(1));
        outboundSmall.setCategorizationStatus(CategorizationStatus.AUTO);
        transactionRepository.save(outboundSmall);

        // Manual transaction - can be deleted by user
        outboundManual = new Transaction();
        outboundManual.setAccount(account);
        outboundManual.setUserId(userId);
        outboundManual.setAmount(new BigDecimal("100.00"));
        outboundManual.setDirection(TransactionDirection.OUTBOUND);
        outboundManual.setCurrency("PLN");
        outboundManual.setBookingDate(LocalDate.now().minusDays(2));
        outboundManual.setCategorizationStatus(CategorizationStatus.MANUAL);
        outboundManual.setManual(true);
        transactionRepository.save(outboundManual);

        // Transaction for otherUserId - should never appear in userId queries
        BankConnection otherConn = new BankConnection();
        otherConn.setUserId(otherUserId);
        otherConn.setInstitutionId("ING_PL");
        otherConn.setInstitutionName("ING");
        otherConn.setCredentialsId("req-ing-001");
        otherConn.setStatus(ConnectionStatus.ACTIVE);
        bankConnectionRepository.save(otherConn);

        BankAccount otherAccount = new BankAccount();
        otherAccount.setConnection(otherConn);
        otherAccount.setUserId(otherUserId);
        otherAccount.setExternalAccountId("ext-other");
        otherAccount.setName("Other Account");
        otherAccount.setCurrency("EUR");
        otherAccount.setActive(true);
        bankAccountRepository.save(otherAccount);

        Transaction otherUserTx = new Transaction();
        otherUserTx.setAccount(otherAccount);
        otherUserTx.setUserId(otherUserId);
        otherUserTx.setAmount(new BigDecimal("50.00"));
        otherUserTx.setDirection(TransactionDirection.OUTBOUND);
        otherUserTx.setCurrency("EUR");
        otherUserTx.setBookingDate(LocalDate.now());
        otherUserTx.setCategorizationStatus(CategorizationStatus.UNCATEGORIZED);
        transactionRepository.save(otherUserTx);
    }

    // findByIdAndUserId

    @Test
    public void findByIdAndUserId_returnsTransaction_whenOwnershipMatches() {
        Optional<Transaction> result = transactionRepository
                .findByIdAndUserId(inboundLarge.getId(), userId);

        assertThat(result).isPresent();
        assertThat(result.get().getAmount()).isEqualByComparingTo("5000.00");
    }

    @Test
    public void findByIdAndUserId_returnsEmpty_whenWrongUserId() {
        Optional<Transaction> result = transactionRepository
                .findByIdAndUserId(inboundLarge.getId(), otherUserId);

        assertThat(result).isEmpty();
    }

    @Test
    public void findByIdAndUserId_returnsEmpty_whenTransactionDoesNotExist() {
        Optional<Transaction> result = transactionRepository
                .findByIdAndUserId(UUID.randomUUID(), userId);

        assertThat(result).isEmpty();
    }

    // findByAccountIdAndExternalId

    @Test
    public void findByAccountIdAndExternalId_returnsTransaction_forDeduplication() {
        Optional<Transaction> result = transactionRepository
                .findByAccountIdAndExternalId(account.getId(), "ext-tx-001");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(inboundLarge.getId());
    }

    @Test
    public void findByAccountIdAndExternalId_returnsEmpty_whenNotFound() {
        Optional<Transaction> result = transactionRepository
                .findByAccountIdAndExternalId(account.getId(), "ext-nonexistent");

        assertThat(result).isEmpty();
    }

    // findFiltered

    @Test
    public void findFiltered_noFilters_returnsAllTransactionsForUser() {
        Page<Transaction> result = transactionRepository.findFiltered(
                userId, null, null, null, null, null, null, null,
                PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).allMatch(t -> t.getUserId().equals(userId));
    }

    @Test
    public void findFiltered_doesNotReturnOtherUsersTransactions() {
        Page<Transaction> result = transactionRepository.findFiltered(
                userId, null, null, null, null, null, null, null,
                PageRequest.of(0, 10));

        assertThat(result.getContent()).noneMatch(t -> t.getUserId().equals(otherUserId));
    }

    @Test
    public void findFiltered_byDirection_returnsOnlyInbound() {
        Page<Transaction> result = transactionRepository.findFiltered(
                userId, null, null, TransactionDirection.INBOUND,
                null, null, null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).allMatch(t -> t.getDirection() == TransactionDirection.INBOUND);
    }

    @Test
    public void findFiltered_byDirection_returnsOnlyOutbound() {
        Page<Transaction> result = transactionRepository.findFiltered(
                userId, null, null, TransactionDirection.OUTBOUND,
                null, null, null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(t -> t.getDirection() == TransactionDirection.OUTBOUND);
    }

    @Test
    public void findFiltered_byMinAmount_returnsOnlyAboveThreshold() {
        Page<Transaction> result = transactionRepository.findFiltered(
                userId, null, null, null,
                null, null, new BigDecimal("100.00"), null, PageRequest.of(0, 10));

        assertThat(result.getContent()).allMatch(t -> t.getAmount().compareTo(new BigDecimal("100.00")) >= 0);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    public void findFiltered_byMaxAmount_returnsOnlyBelowThreshold() {
        Page<Transaction> result = transactionRepository.findFiltered(
                userId, null, null, null,
                null, null, null, new BigDecimal("100.00"), PageRequest.of(0, 10));

        assertThat(result.getContent()).allMatch(t -> t.getAmount().compareTo(new BigDecimal("100.00")) <= 0);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    public void findFiltered_byDateRange_returnsOnlyInRange() {
        Page<Transaction> result = transactionRepository.findFiltered(
                userId, null, null, null,
                LocalDate.now().minusDays(1), LocalDate.now(),
                null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    public void findFiltered_orderedByBookingDateDesc() {
        Page<Transaction> result = transactionRepository.findFiltered(
                userId, null, null, null, null, null, null, null,
                PageRequest.of(0, 10));

        List<LocalDate> dates = result.getContent().stream()
                .map(Transaction::getBookingDate).toList();
        for (int i = 0; i < dates.size() - 1; i++) {
            assertThat(dates.get(i)).isAfterOrEqualTo(dates.get(i + 1));
        }
    }

    // findByUserIdAndCategorizationStatus

    @Test
    public void findByUserIdAndCategorizationStatus_returnsUncategorized() {
        Page<Transaction> result = transactionRepository.findByUserIdAndCategorizationStatus(
                userId, CategorizationStatus.UNCATEGORIZED, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(inboundLarge.getId());
    }

    @Test
    public void findByUserIdAndCategorizationStatus_returnsManual() {
        Page<Transaction> result = transactionRepository.findByUserIdAndCategorizationStatus(
                userId, CategorizationStatus.MANUAL, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).isManual()).isTrue();
    }
}