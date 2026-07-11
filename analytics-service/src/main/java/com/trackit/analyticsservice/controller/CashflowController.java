package com.trackit.analyticsservice.controller;

import com.trackit.analyticsservice.dto.response.CashflowSummaryResponseDTO;
import com.trackit.analyticsservice.service.CashflowSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics/cashflow")
@RequiredArgsConstructor
@Tag(name = "Cashflow", description = "Monthly income and expense summaries aggregated from bank transactions")
public class CashflowController {

    private final CashflowSyncService service;

    // GET /api/analytics/cashflow
    @GetMapping
    @Operation(summary = "Get all cashflow summaries for user, ordered newest first")
    public ResponseEntity<List<CashflowSummaryResponseDTO>> getAll(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(service.getAll(userId));
    }

    // GET /api/analytics/cashflow/{year}/{month}
    @GetMapping("/{year}/{month}")
    @Operation(summary = "Get cashflow summary for a specific month")
    public ResponseEntity<CashflowSummaryResponseDTO> getByPeriod(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable int year,
            @PathVariable int month) {
        return ResponseEntity.ok(service.getByPeriod(userId, year, month));
    }

    // POST /api/analytics/cashflow/sync/{year}/{month}
    @PostMapping("/sync/{year}/{month}")
    @Operation(summary = "Manually trigger cashflow sync for a specific month")
    public ResponseEntity<Void> sync(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable int year,
            @PathVariable int month) {
        service.sync(userId, year, month);
        return ResponseEntity.noContent().build();
    }
}