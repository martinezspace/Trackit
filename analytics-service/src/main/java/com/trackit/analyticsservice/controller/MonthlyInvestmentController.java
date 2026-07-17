package com.trackit.analyticsservice.controller;

import com.trackit.analyticsservice.dto.response.MonthlyInvestmentSummaryResponseDTO;
import com.trackit.analyticsservice.service.MonthlyInvestmentSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics/monthly-investments")
@RequiredArgsConstructor
@Tag(name = "Monthly Investments", description = "Monthly investment activity summaries per account")
public class MonthlyInvestmentController {

    private final MonthlyInvestmentSyncService service;

    // GET /api/analytics/monthly-investments
    @GetMapping
    @Operation(summary = "Get all monthly investment summaries for user, ordered newest first")
    public ResponseEntity<List<MonthlyInvestmentSummaryResponseDTO>> getAll(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(service.getAll(userId));
    }

    // GET /api/analytics/monthly-investments/account/{accountId}
    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get all monthly summaries for a specific investment account")
    public ResponseEntity<List<MonthlyInvestmentSummaryResponseDTO>> getByAccount(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID accountId) {
        return ResponseEntity.ok(service.getByAccount(userId, accountId));
    }

    // GET /api/analytics/monthly-investments/account/{accountId}/{year}/{month}
    @GetMapping("/account/{accountId}/{year}/{month}")
    @Operation(summary = "Get monthly summary for a specific account and period")
    public ResponseEntity<MonthlyInvestmentSummaryResponseDTO> getByAccountAndPeriod(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID accountId,
            @PathVariable int year,
            @PathVariable int month) {
        return ResponseEntity.ok(service.getByAccountAndPeriod(userId, accountId, year, month));
    }

    // POST /api/analytics/monthly-investments/sync/{year}/{month}
    @PostMapping("/sync/{year}/{month}")
    @Operation(summary = "Manually trigger monthly investment sync for a specific month")
    public ResponseEntity<Void> sync(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable int year,
            @PathVariable int month) {
        service.sync(userId, year, month);
        return ResponseEntity.noContent().build();
    }
}