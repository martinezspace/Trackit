package com.trackit.analyticsservice.repository;

import com.trackit.analyticsservice.model.CategorySpendingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategorySpendingSummaryRepository extends JpaRepository<CategorySpendingSummary, UUID> {

    List<CategorySpendingSummary> findByUserIdAndPeriodYearAndPeriodMonthOrderByTotalAmountDesc(
            UUID userId, int year, int month);

    // Used by sync to upsert per category per period
    Optional<CategorySpendingSummary> findByUserIdAndCategoryIdAndPeriodYearAndPeriodMonth(
            UUID userId, UUID categoryId, int year, int month);
}