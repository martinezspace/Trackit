package com.trackit.analyticsservice.repository;

import com.trackit.analyticsservice.model.MonthlyInvestmentSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonthlyInvestmentSummaryRepository extends JpaRepository<MonthlyInvestmentSummary, UUID> {

    List<MonthlyInvestmentSummary> findByUserIdOrderByPeriodYearDescPeriodMonthDesc(UUID userId);

    List<MonthlyInvestmentSummary> findByUserIdAndAccountIdOrderByPeriodYearDescPeriodMonthDesc(
            UUID userId, UUID accountId);

    // Used by sync to upsert per account per period
    Optional<MonthlyInvestmentSummary> findByUserIdAndAccountIdAndPeriodYearAndPeriodMonth(
            UUID userId, UUID accountId, int year, int month);
}