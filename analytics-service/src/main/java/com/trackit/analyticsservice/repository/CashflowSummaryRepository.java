package com.trackit.analyticsservice.repository;

import com.trackit.analyticsservice.model.CashflowSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashflowSummaryRepository extends JpaRepository<CashflowSummary, UUID> {

    // Used by sync to upsert — check existence before deciding insert vs update
    Optional<CashflowSummary> findByUserIdAndPeriodYearAndPeriodMonth(UUID userId, int year, int month);

    List<CashflowSummary> findByUserIdOrderByPeriodYearDescPeriodMonthDesc(UUID userId);
}