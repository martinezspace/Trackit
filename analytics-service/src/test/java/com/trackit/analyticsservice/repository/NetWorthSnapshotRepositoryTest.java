package com.trackit.analyticsservice.repository;

import com.trackit.analyticsservice.model.NetWorthSnapshot;
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
class NetWorthSnapshotRepositoryTest {

    @Autowired
    private NetWorthSnapshotRepository repository;

    private UUID userId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        repository.save(buildSnapshot(userId, LocalDate.of(2024, 1, 1), "1000.00", "500.00"));
        repository.save(buildSnapshot(userId, LocalDate.of(2024, 2, 1), "1200.00", "600.00"));
        repository.save(buildSnapshot(userId, LocalDate.of(2024, 3, 1), "1100.00", "550.00"));
        repository.save(buildSnapshot(otherUserId, LocalDate.of(2024, 3, 1), "9000.00", "4000.00"));
    }

    // findByUserIdOrderBySnapshotDateAsc

    @Test
    void findByUserIdOrderBySnapshotDateAsc_returnsOnlyUserSnapshots() {
        List<NetWorthSnapshot> result = repository.findByUserIdOrderBySnapshotDateAsc(userId);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(s -> s.getUserId().equals(userId));
    }

    @Test
    void findByUserIdOrderBySnapshotDateAsc_returnsInChronologicalOrder() {
        List<NetWorthSnapshot> result = repository.findByUserIdOrderBySnapshotDateAsc(userId);

        assertThat(result.get(0).getSnapshotDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(result.get(1).getSnapshotDate()).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(result.get(2).getSnapshotDate()).isEqualTo(LocalDate.of(2024, 3, 1));
    }

    // findByUserIdAndSnapshotDateBetween

    @Test
    void findByUserIdAndSnapshotDateBetween_returnsOnlySnapshotsInRange() {
        List<NetWorthSnapshot> result = repository
                .findByUserIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                        userId, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 1));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSnapshotDate()).isEqualTo(LocalDate.of(2024, 2, 1));
    }

    @Test
    void findByUserIdAndSnapshotDateBetween_returnsEmpty_whenNoSnapshotsInRange() {
        List<NetWorthSnapshot> result = repository
                .findByUserIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                        userId, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31));

        assertThat(result).isEmpty();
    }

    // findTopByUserIdOrderBySnapshotDateDesc

    @Test
    void findTopByUserIdOrderBySnapshotDateDesc_returnsLatestSnapshot() {
        Optional<NetWorthSnapshot> result = repository.findTopByUserIdOrderBySnapshotDateDesc(userId);

        assertThat(result).isPresent();
        assertThat(result.get().getSnapshotDate()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(result.get().getNetWorth()).isEqualByComparingTo("1650.00");
    }

    @Test
    void findTopByUserIdOrderBySnapshotDateDesc_returnsEmpty_whenNoSnapshots() {
        Optional<NetWorthSnapshot> result = repository
                .findTopByUserIdOrderBySnapshotDateDesc(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    // findByUserIdAndSnapshotDate

    @Test
    void findByUserIdAndSnapshotDate_returnsSnapshot_whenExists() {
        Optional<NetWorthSnapshot> result = repository
                .findByUserIdAndSnapshotDate(userId, LocalDate.of(2024, 1, 1));

        assertThat(result).isPresent();
        assertThat(result.get().getBankBalanceTotal()).isEqualByComparingTo("1000.00");
    }

    @Test
    void findByUserIdAndSnapshotDate_returnsEmpty_whenNotExists() {
        Optional<NetWorthSnapshot> result = repository
                .findByUserIdAndSnapshotDate(userId, LocalDate.of(2099, 1, 1));

        assertThat(result).isEmpty();
    }

    @Test
    void findByUserIdAndSnapshotDate_returnsEmpty_whenWrongUser() {
        Optional<NetWorthSnapshot> result = repository
                .findByUserIdAndSnapshotDate(otherUserId, LocalDate.of(2024, 1, 1));

        assertThat(result).isEmpty();
    }

    private NetWorthSnapshot buildSnapshot(UUID userId, LocalDate date,
                                           String bankTotal, String investmentTotal) {
        NetWorthSnapshot s = new NetWorthSnapshot();
        s.setUserId(userId);
        s.setSnapshotDate(date);
        s.setBankBalanceTotal(new BigDecimal(bankTotal));
        s.setInvestmentValueTotal(new BigDecimal(investmentTotal));
        s.setNetWorth(new BigDecimal(bankTotal).add(new BigDecimal(investmentTotal)));
        s.setCurrency("EUR");
        return s;
    }
}