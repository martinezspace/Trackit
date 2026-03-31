package com.trackit.investmentservice.repository;

import com.trackit.investmentservice.model.AccountType;
import com.trackit.investmentservice.model.InvestmentAccount;
import com.trackit.investmentservice.model.PortfolioSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PortfolioSnapshotRepositoryTest {

    @Autowired
    private PortfolioSnapshotRepository snapshotRepository;

    @Autowired
    private InvestmentAccountRepository investmentAccountRepository;

    private InvestmentAccount account;
    private InvestmentAccount otherAccount;

    @BeforeEach
    void setUp() {
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

        // Three snapshots for account
        snapshotRepository.save(buildSnapshot(account, LocalDate.of(2024, 1, 31),
                "10500.00", "10000.00", "500.00", "5.0000"));
        snapshotRepository.save(buildSnapshot(account, LocalDate.of(2024, 2, 29),
                "11200.00", "10500.00", "700.00", "6.6667"));
        snapshotRepository.save(buildSnapshot(account, LocalDate.of(2024, 3, 31),
                "10800.00", "10500.00", "300.00", "2.8571"));

        // One snapshot for otherAccount
        snapshotRepository.save(buildSnapshot(otherAccount, LocalDate.of(2024, 3, 31),
                "5000.00", "4500.00", "500.00", "11.1111"));
    }

    // findByAccount_IdOrderBySnapshotDateAsc
    @Test
    void findByAccount_IdOrderBySnapshotDateAsc_returnsOnlyAccountSnapshots() {
        List<PortfolioSnapshot> result = snapshotRepository
                .findByAccount_IdOrderBySnapshotDateAsc(account.getId());

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(s -> s.getAccount().getId().equals(account.getId()));
    }

    @Test
    void findByAccount_IdOrderBySnapshotDateAsc_returnsInChronologicalOrder() {
        List<PortfolioSnapshot> result = snapshotRepository
                .findByAccount_IdOrderBySnapshotDateAsc(account.getId());

        assertThat(result.get(0).getSnapshotDate()).isEqualTo(LocalDate.of(2024, 1, 31));
        assertThat(result.get(1).getSnapshotDate()).isEqualTo(LocalDate.of(2024, 2, 29));
        assertThat(result.get(2).getSnapshotDate()).isEqualTo(LocalDate.of(2024, 3, 31));
    }

    // findByAccount_IdAndSnapshotDateBetween
    @Test
    void findByAccount_IdAndSnapshotDateBetween_returnsOnlySnapshotsInRange() {
        LocalDate from = LocalDate.of(2024, 2, 1);
        LocalDate to = LocalDate.of(2024, 3, 31);

        List<PortfolioSnapshot> result = snapshotRepository
                .findByAccount_IdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                        account.getId(), from, to);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSnapshotDate()).isEqualTo(LocalDate.of(2024, 2, 29));
        assertThat(result.get(1).getSnapshotDate()).isEqualTo(LocalDate.of(2024, 3, 31));
    }

    @Test
    void findByAccount_IdAndSnapshotDateBetween_returnsEmpty_whenNoSnapshotsInRange() {
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to = LocalDate.of(2020, 12, 31);

        List<PortfolioSnapshot> result = snapshotRepository
                .findByAccount_IdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                        account.getId(), from, to);

        assertThat(result).isEmpty();
    }

    // findTopByAccount_IdOrderBySnapshotDateDesc
    @Test
    void findTopByAccount_IdOrderBySnapshotDateDesc_returnsLatestSnapshot() {
        Optional<PortfolioSnapshot> result = snapshotRepository
                .findTopByAccount_IdOrderBySnapshotDateDesc(account.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getSnapshotDate()).isEqualTo(LocalDate.of(2024, 3, 31));
        assertThat(result.get().getTotalValue()).isEqualByComparingTo(new BigDecimal("10800.00"));
    }

    @Test
    void findTopByAccount_IdOrderBySnapshotDateDesc_returnsEmpty_whenNoSnapshots() {
        InvestmentAccount emptyAccount = new InvestmentAccount();
        emptyAccount.setUserId(UUID.randomUUID());
        emptyAccount.setAccountType(AccountType.IKE);
        emptyAccount.setBrokerName("mBank");
        emptyAccount.setCurrency("PLN");
        emptyAccount.setActive(true);
        investmentAccountRepository.save(emptyAccount);

        Optional<PortfolioSnapshot> result = snapshotRepository
                .findTopByAccount_IdOrderBySnapshotDateDesc(emptyAccount.getId());

        assertThat(result).isEmpty();
    }

    // existsByAccount_IdAndSnapshotDate
    @Test
    void existsByAccount_IdAndSnapshotDate_returnsTrue_whenExists() {
        boolean result = snapshotRepository
                .existsByAccount_IdAndSnapshotDate(account.getId(), LocalDate.of(2024, 3, 31));

        assertThat(result).isTrue();
    }

    @Test
    void existsByAccount_IdAndSnapshotDate_returnsFalse_whenNotExists() {
        boolean result = snapshotRepository
                .existsByAccount_IdAndSnapshotDate(account.getId(), LocalDate.of(2024, 4, 30));

        assertThat(result).isFalse();
    }

    @Test
    void existsByAccount_IdAndSnapshotDate_returnsFalse_whenWrongAccount() {
        boolean result = snapshotRepository
                .existsByAccount_IdAndSnapshotDate(otherAccount.getId(), LocalDate.of(2024, 1, 31));

        assertThat(result).isFalse();
    }

    // Private helpers
    private PortfolioSnapshot buildSnapshot(InvestmentAccount account, LocalDate date,
                                            String totalValue, String totalInvested, String gainLoss, String gainLossPct) {
        PortfolioSnapshot snapshot = new PortfolioSnapshot();
        snapshot.setAccount(account);
        snapshot.setSnapshotDate(date);
        snapshot.setTotalValue(new BigDecimal(totalValue));
        snapshot.setTotalInvested(new BigDecimal(totalInvested));
        snapshot.setTotalGainLoss(new BigDecimal(gainLoss));
        snapshot.setGainLossPct(new BigDecimal(gainLossPct));
        snapshot.setCurrency("PLN");
        return snapshot;
    }
}