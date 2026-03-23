package com.trackit.investmentservice.repository;

import com.trackit.investmentservice.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ImportBatchRepositoryTest {

    @Autowired
    private ImportBatchRepository importBatchRepository;

    @Autowired
    private InvestmentAccountRepository investmentAccountRepository;

    private InvestmentAccount account;
    private InvestmentAccount otherAccount;
    private ImportBatch pendingBatch;
    private ImportBatch completedBatch;

    @BeforeEach
    void setUp() {
        // Create two accounts
        account = new InvestmentAccount();
        account.setUserId(UUID.randomUUID());
        account.setAccountType(AccountType.BROKERAGE);
        account.setBrokerName("Trading212");
        account.setCurrency("PLN");
        account.setActive(true);
        investmentAccountRepository.save(account);

        otherAccount = new InvestmentAccount();
        otherAccount.setUserId(UUID.randomUUID());
        otherAccount.setAccountType(AccountType.BROKERAGE);
        otherAccount.setBrokerName("XTB");
        otherAccount.setCurrency("PLN");
        otherAccount.setActive(true);
        investmentAccountRepository.save(otherAccount);

        // Pending batch for account
        pendingBatch = new ImportBatch();
        pendingBatch.setAccount(account);
        pendingBatch.setBrokerFormat(BrokerFormat.TRADING212);
        pendingBatch.setFilename("january.csv");
        pendingBatch.setStatus(ImportStatus.PENDING);
        importBatchRepository.save(pendingBatch);

        // Completed batch for account
        completedBatch = new ImportBatch();
        completedBatch.setAccount(account);
        completedBatch.setBrokerFormat(BrokerFormat.TRADING212);
        completedBatch.setFilename("february.csv");
        completedBatch.setStatus(ImportStatus.COMPLETED);
        importBatchRepository.save(completedBatch);

        // Batch for other account — should never appear in account's results
        ImportBatch otherBatch = new ImportBatch();
        otherBatch.setAccount(otherAccount);
        otherBatch.setBrokerFormat(BrokerFormat.XTB);
        otherBatch.setFilename("other.csv");
        otherBatch.setStatus(ImportStatus.PENDING);
        importBatchRepository.save(otherBatch);
    }

    // findByAccount_IdOrderByCreatedAtDesc

    @Test
    void findByAccount_Id_returnsOnlyBatchesForThatAccount() {
        List<ImportBatch> result = importBatchRepository
                .findByAccount_IdOrderByCreatedAtDesc(account.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(b -> b.getAccount().getId().equals(account.getId()));
    }

    @Test
    void findByAccount_Id_returnsEmpty_whenAccountHasNoBatches() {
        InvestmentAccount emptyAccount = new InvestmentAccount();
        emptyAccount.setUserId(UUID.randomUUID());
        emptyAccount.setAccountType(AccountType.IKE);
        emptyAccount.setBrokerName("mBank");
        emptyAccount.setCurrency("PLN");
        emptyAccount.setActive(true);
        investmentAccountRepository.save(emptyAccount);

        List<ImportBatch> result = importBatchRepository
                .findByAccount_IdOrderByCreatedAtDesc(emptyAccount.getId());

        assertThat(result).isEmpty();
    }

    // findByIdAndAccount_Id

    @Test
    void findByIdAndAccount_Id_returnsBatch_whenOwnershipMatches() {
        Optional<ImportBatch> result = importBatchRepository
                .findByIdAndAccount_Id(pendingBatch.getId(), account.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getFilename()).isEqualTo("january.csv");
    }

    @Test
    void findByIdAndAccount_Id_returnsEmpty_whenWrongAccount() {
        Optional<ImportBatch> result = importBatchRepository
                .findByIdAndAccount_Id(pendingBatch.getId(), otherAccount.getId());

        assertThat(result).isEmpty();
    }

    // findByStatus

    @Test
    void findByStatus_returnsOnlyMatchingStatus() {
        List<ImportBatch> result = importBatchRepository.findByStatus(ImportStatus.PENDING);

        // Two pending batches — one for account, one for otherAccount
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(b -> b.getStatus() == ImportStatus.PENDING);
    }

    @Test
    void findByStatus_returnsEmpty_whenNoneMatch() {
        List<ImportBatch> result = importBatchRepository.findByStatus(ImportStatus.FAILED);

        assertThat(result).isEmpty();
    }

    // existsByAccount_IdAndStatusIn

    @Test
    void existsByAccount_IdAndStatusIn_returnsTrue_whenActiveImportExists() {
        boolean result = importBatchRepository.existsByAccount_IdAndStatusIn(
                account.getId(),
                List.of(ImportStatus.PENDING, ImportStatus.PROCESSING)
        );

        assertThat(result).isTrue();
    }

    @Test
    void existsByAccount_IdAndStatusIn_returnsFalse_whenNoActiveImport() {
        boolean result = importBatchRepository.existsByAccount_IdAndStatusIn(
                account.getId(),
                List.of(ImportStatus.PROCESSING)  // account only has PENDING, not PROCESSING
        );

        assertThat(result).isFalse();
    }
}