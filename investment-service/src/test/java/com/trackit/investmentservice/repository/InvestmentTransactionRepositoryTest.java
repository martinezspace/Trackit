package com.trackit.investmentservice.repository;

import com.trackit.investmentservice.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class InvestmentTransactionRepositoryTest {

    @Autowired
    private InvestmentTransactionRepository transactionRepository;

    @Autowired
    private InvestmentAccountRepository investmentAccountRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private ImportBatchRepository importBatchRepository;

    private InvestmentAccount account;
    private InvestmentAccount otherAccount;
    private Instrument instrument;
    private ImportBatch batch;
    private InvestmentTransaction activeBuyTransaction;
    private InvestmentTransaction activeSellTransaction;
    private InvestmentTransaction cancelledTransaction;

    @BeforeEach
    void setUp() {
        account = new InvestmentAccount();
        account.setUserId(java.util.UUID.randomUUID());
        account.setAccountType(AccountType.BROKERAGE);
        account.setBrokerName("Trading212");
        account.setCurrency("PLN");
        account.setActive(true);
        investmentAccountRepository.save(account);

        otherAccount = new InvestmentAccount();
        otherAccount.setUserId(java.util.UUID.randomUUID());
        otherAccount.setAccountType(AccountType.BROKERAGE);
        otherAccount.setBrokerName("XTB");
        otherAccount.setCurrency("PLN");
        otherAccount.setActive(true);
        investmentAccountRepository.save(otherAccount);

        instrument = new Instrument();
        instrument.setIsin("IE00B5BMR087");
        instrument.setTicker("CSPX");
        instrument.setName("iShares Core S&P 500");
        instrument.setInstrumentType(InstrumentType.ETF);
        instrument.setCurrency("USD");
        instrumentRepository.save(instrument);

        batch = new ImportBatch();
        batch.setAccount(account);
        batch.setBrokerFormat(BrokerFormat.TRADING212);
        batch.setFilename("january.csv");
        batch.setStatus(ImportStatus.COMPLETED);
        importBatchRepository.save(batch);

        // Active BUY transaction
        activeBuyTransaction = new InvestmentTransaction();
        activeBuyTransaction.setAccount(account);
        activeBuyTransaction.setInstrument(instrument);
        activeBuyTransaction.setBatch(batch);
        activeBuyTransaction.setExternalId("EOF9554472121");
        activeBuyTransaction.setTransactionType(TransactionType.BUY);
        activeBuyTransaction.setQuantity(new BigDecimal("1.150000"));
        activeBuyTransaction.setPrice(new BigDecimal("516.7500"));
        activeBuyTransaction.setAmount(new BigDecimal("2399.99"));
        activeBuyTransaction.setCurrency("PLN");
        activeBuyTransaction.setTransactionDate(LocalDate.of(2024, 1, 30));
        activeBuyTransaction.setCancelled(false);
        transactionRepository.save(activeBuyTransaction);

        // Active SELL transaction
        activeSellTransaction = new InvestmentTransaction();
        activeSellTransaction.setAccount(account);
        activeSellTransaction.setInstrument(instrument);
        activeSellTransaction.setBatch(batch);
        activeSellTransaction.setExternalId("EOF9604063567");
        activeSellTransaction.setTransactionType(TransactionType.SELL);
        activeSellTransaction.setQuantity(new BigDecimal("1.150000"));
        activeSellTransaction.setPrice(new BigDecimal("514.3600"));
        activeSellTransaction.setAmount(new BigDecimal("2363.08"));
        activeSellTransaction.setCurrency("PLN");
        activeSellTransaction.setTransactionDate(LocalDate.of(2024, 1, 31));
        activeSellTransaction.setCancelled(false);
        transactionRepository.save(activeSellTransaction);

        // Cancelled transaction
        cancelledTransaction = new InvestmentTransaction();
        cancelledTransaction.setAccount(account);
        cancelledTransaction.setInstrument(instrument);
        cancelledTransaction.setExternalId("EOF9604063999");
        cancelledTransaction.setTransactionType(TransactionType.BUY);
        cancelledTransaction.setQuantity(new BigDecimal("0.500000"));
        cancelledTransaction.setPrice(new BigDecimal("516.7500"));
        cancelledTransaction.setAmount(new BigDecimal("1000.00"));
        cancelledTransaction.setCurrency("PLN");
        cancelledTransaction.setTransactionDate(LocalDate.of(2024, 1, 29));
        cancelledTransaction.setCancelled(true);
        transactionRepository.save(cancelledTransaction);
    }

    // findByAccount_IdAndCancelledFalse
    @Test
    void findByAccount_IdAndCancelledFalse_returnsOnlyActiveTransactions() {
        List<InvestmentTransaction> result =
                transactionRepository.findByAccount_IdAndCancelledFalse(account.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> !t.isCancelled());
    }

    @Test
    void findByAccount_IdAndCancelledFalse_excludesCancelledTransactions() {
        List<InvestmentTransaction> result =
                transactionRepository.findByAccount_IdAndCancelledFalse(account.getId());

        assertThat(result).noneMatch(t -> t.getExternalId().equals("EOF9604063999"));
    }

    // findByAccount_IdAndInstrument_IdAndCancelledFalse
    @Test
    void findByAccount_IdAndInstrument_IdAndCancelledFalse_returnsCorrectTransactions() {
        List<InvestmentTransaction> result = transactionRepository
                .findByAccount_IdAndInstrument_IdAndCancelledFalse(account.getId(), instrument.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.getInstrument().getId().equals(instrument.getId()));
    }

    // findByIdAndAccount_Id
    @Test
    void findByIdAndAccount_Id_returnsTransaction_whenOwnershipMatches() {
        Optional<InvestmentTransaction> result = transactionRepository
                .findByIdAndAccount_Id(activeBuyTransaction.getId(), account.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getExternalId()).isEqualTo("EOF9554472121");
    }

    @Test
    void findByIdAndAccount_Id_returnsEmpty_whenWrongAccount() {
        Optional<InvestmentTransaction> result = transactionRepository
                .findByIdAndAccount_Id(activeBuyTransaction.getId(), otherAccount.getId());

        assertThat(result).isEmpty();
    }

    // existsByAccount_IdAndExternalId
    @Test
    void existsByAccount_IdAndExternalId_returnsTrue_whenExists() {
        boolean result = transactionRepository
                .existsByAccount_IdAndExternalId(account.getId(), "EOF9554472121");

        assertThat(result).isTrue();
    }

    @Test
    void existsByAccount_IdAndExternalId_returnsFalse_whenNotExists() {
        boolean result = transactionRepository
                .existsByAccount_IdAndExternalId(account.getId(), "NONEXISTENT");

        assertThat(result).isFalse();
    }

    @Test
    void existsByAccount_IdAndExternalId_returnsFalse_whenWrongAccount() {
        boolean result = transactionRepository
                .existsByAccount_IdAndExternalId(otherAccount.getId(), "EOF9554472121");

        assertThat(result).isFalse();
    }

    // findByBatch_Id
    @Test
    void findByBatch_Id_returnsAllBatchTransactions() {
        List<InvestmentTransaction> result =
                transactionRepository.findByBatch_Id(batch.getId());

        // activeBuyTransaction and activeSellTransaction belong to batch
        // cancelledTransaction has no batch
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.getBatch().getId().equals(batch.getId()));
    }

    @Test
    void findByBatch_Id_returnsEmpty_whenNoBatchTransactions() {
        ImportBatch emptyBatch = new ImportBatch();
        emptyBatch.setAccount(account);
        emptyBatch.setBrokerFormat(BrokerFormat.TRADING212);
        emptyBatch.setFilename("empty.csv");
        emptyBatch.setStatus(ImportStatus.COMPLETED);
        importBatchRepository.save(emptyBatch);

        List<InvestmentTransaction> result =
                transactionRepository.findByBatch_Id(emptyBatch.getId());

        assertThat(result).isEmpty();
    }
}