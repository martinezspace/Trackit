package com.trackit.analyticsservice.repository;

import com.trackit.analyticsservice.model.CashflowSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CashflowSummaryRepositoryTest {

    @Autowired
    private CashflowSummaryRepository repository;

    private UUID userId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        repository.save(buildSummary(userId, 2024, 1, "3000.00", "1800.00"));
        repository.save(buildSummary(userId, 2024, 2, "3200.00", "2100.00"));
        repository.save(buildSummary(userId, 2024, 3, "2900.00", "1700.00"));
        repository.save(buildSummary(otherUserId, 2024, 3, "5000.00", "3000.00"));
    }

    // findByUserIdAndPeriodYearAndPeriodMonth

    @Test
    void findByUserIdAndPeriodYearAndPeriodMonth_returnsSummary_whenExists() {
        Optional<CashflowSummary> result = repository
                .findByUserIdAndPeriodYearAndPeriodMonth(userId, 2024, 2);

        assertThat(result).isPresent();
        assertThat(result.get().getTotalIncome()).isEqualByComparingTo("3200.00");
    }

    @Test
    void findByUserIdAndPeriodYearAndPeriodMonth_returnsEmpty_whenWrongUser() {
        Optional<CashflowSummary> result = repository
                .findByUserIdAndPeriodYearAndPeriodMonth(otherUserId, 2024, 1);

        assertThat(result).isEmpty();
    }

    @Test
    void findByUserIdAndPeriodYearAndPeriodMonth_returnsEmpty_whenPeriodNotFound() {
        Optional<CashflowSummary> result = repository
                .findByUserIdAndPeriodYearAndPeriodMonth(userId, 2020, 1);

        assertThat(result).isEmpty();
    }

    // findByUserIdOrderByPeriodYearDescPeriodMonthDesc

    @Test
    void findByUserIdOrderByPeriodYearDescPeriodMonthDesc_returnsOnlyUserSummaries() {
        List<CashflowSummary> result = repository
                .findByUserIdOrderByPeriodYearDescPeriodMonthDesc(userId);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(s -> s.getUserId().equals(userId));
    }

    @Test
    void findByUserIdOrderByPeriodYearDescPeriodMonthDesc_returnsNewestFirst() {
        List<CashflowSummary> result = repository
                .findByUserIdOrderByPeriodYearDescPeriodMonthDesc(userId);

        assertThat(result.get(0).getPeriodMonth()).isEqualTo(3);
        assertThat(result.get(1).getPeriodMonth()).isEqualTo(2);
        assertThat(result.get(2).getPeriodMonth()).isEqualTo(1);
    }

    @Test
    void findByUserIdOrderByPeriodYearDescPeriodMonthDesc_returnsEmpty_whenNoSummaries() {
        List<CashflowSummary> result = repository
                .findByUserIdOrderByPeriodYearDescPeriodMonthDesc(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    private CashflowSummary buildSummary(UUID userId, int year, int month,
                                         String income, String expenses) {
        CashflowSummary s = new CashflowSummary();
        s.setUserId(userId);
        s.setPeriodYear(year);
        s.setPeriodMonth(month);
        s.setTotalIncome(new BigDecimal(income));
        s.setTotalExpenses(new BigDecimal(expenses));
        s.setNetCashflow(new BigDecimal(income).subtract(new BigDecimal(expenses)));
        s.setTransactionCount(10);
        s.setCurrency("EUR");
        return s;
    }
}