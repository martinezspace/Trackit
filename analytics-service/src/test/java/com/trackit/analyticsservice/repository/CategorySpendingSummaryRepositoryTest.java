package com.trackit.analyticsservice.repository;

import com.trackit.analyticsservice.model.CategorySpendingSummary;
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
class CategorySpendingSummaryRepositoryTest {

    @Autowired
    private CategorySpendingSummaryRepository repository;

    private UUID userId;
    private UUID groceriesId;
    private UUID transportId;
    private UUID diningId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        groceriesId = UUID.randomUUID();
        transportId = UUID.randomUUID();
        diningId = UUID.randomUUID();

        repository.save(buildSummary(userId, groceriesId, "Groceries", 2024, 3, "400.00"));
        repository.save(buildSummary(userId, transportId, "Transport", 2024, 3, "150.00"));
        repository.save(buildSummary(userId, diningId, "Dining", 2024, 3, "250.00"));
        // Different period
        repository.save(buildSummary(userId, groceriesId, "Groceries", 2024, 2, "380.00"));
        // Different user
        repository.save(buildSummary(UUID.randomUUID(), groceriesId, "Groceries", 2024, 3, "999.00"));
    }

    // findByUserIdAndPeriodYearAndPeriodMonthOrderByTotalAmountDesc

    @Test
    void findByPeriod_returnsOnlyUserSummariesForPeriod() {
        List<CategorySpendingSummary> result = repository
                .findByUserIdAndPeriodYearAndPeriodMonthOrderByTotalAmountDesc(userId, 2024, 3);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(s -> s.getUserId().equals(userId));
    }

    @Test
    void findByPeriod_returnsOrderedByTotalAmountDesc() {
        List<CategorySpendingSummary> result = repository
                .findByUserIdAndPeriodYearAndPeriodMonthOrderByTotalAmountDesc(userId, 2024, 3);

        assertThat(result.get(0).getCategoryName()).isEqualTo("Groceries");
        assertThat(result.get(1).getCategoryName()).isEqualTo("Dining");
        assertThat(result.get(2).getCategoryName()).isEqualTo("Transport");
    }

    @Test
    void findByPeriod_returnsEmpty_whenNoPeriodMatch() {
        List<CategorySpendingSummary> result = repository
                .findByUserIdAndPeriodYearAndPeriodMonthOrderByTotalAmountDesc(userId, 2020, 1);

        assertThat(result).isEmpty();
    }

    // findByUserIdAndCategoryIdAndPeriodYearAndPeriodMonth

    @Test
    void findByCategoryAndPeriod_returnsSummary_whenExists() {
        Optional<CategorySpendingSummary> result = repository
                .findByUserIdAndCategoryIdAndPeriodYearAndPeriodMonth(userId, groceriesId, 2024, 3);

        assertThat(result).isPresent();
        assertThat(result.get().getTotalAmount()).isEqualByComparingTo("400.00");
    }

    @Test
    void findByCategoryAndPeriod_returnsEmpty_whenWrongPeriod() {
        Optional<CategorySpendingSummary> result = repository
                .findByUserIdAndCategoryIdAndPeriodYearAndPeriodMonth(userId, groceriesId, 2020, 1);

        assertThat(result).isEmpty();
    }

    @Test
    void findByCategoryAndPeriod_returnsEmpty_whenWrongUser() {
        Optional<CategorySpendingSummary> result = repository
                .findByUserIdAndCategoryIdAndPeriodYearAndPeriodMonth(UUID.randomUUID(), groceriesId, 2024, 3);

        assertThat(result).isEmpty();
    }

    private CategorySpendingSummary buildSummary(UUID userId, UUID categoryId, String categoryName,
                                                  int year, int month, String amount) {
        CategorySpendingSummary s = new CategorySpendingSummary();
        s.setUserId(userId);
        s.setCategoryId(categoryId);
        s.setCategoryName(categoryName);
        s.setPeriodYear(year);
        s.setPeriodMonth(month);
        s.setTotalAmount(new BigDecimal(amount));
        s.setTransactionCount(5);
        s.setCurrency("EUR");
        return s;
    }
}