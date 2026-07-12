package com.trackit.analyticsservice.controller;

import com.trackit.analyticsservice.dto.response.CategorySpendingSummaryResponseDTO;
import com.trackit.analyticsservice.service.CategorySpendingSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics/category-spending")
@RequiredArgsConstructor
@Tag(name = "Category Spending", description = "Monthly spending breakdowns by category aggregated from bank transactions")
public class CategorySpendingController {

    private final CategorySpendingSyncService service;

    // GET /api/analytics/category-spending/{year}/{month}
    @GetMapping("/{year}/{month}")
    @Operation(summary = "Get all category spending summaries for a month, ordered by total amount descending")
    public ResponseEntity<List<CategorySpendingSummaryResponseDTO>> getByPeriod(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable int year,
            @PathVariable int month) {
        return ResponseEntity.ok(service.getByPeriod(userId, year, month));
    }

    // GET /api/analytics/category-spending/{year}/{month}/{categoryId}
    @GetMapping("/{year}/{month}/{categoryId}")
    @Operation(summary = "Get spending summary for a specific category and month")
    public ResponseEntity<CategorySpendingSummaryResponseDTO> getByPeriodAndCategory(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable int year,
            @PathVariable int month,
            @PathVariable UUID categoryId) {
        return ResponseEntity.ok(service.getByPeriodAndCategory(userId, year, month, categoryId));
    }

    // POST /api/analytics/category-spending/sync/{year}/{month}
    @PostMapping("/sync/{year}/{month}")
    @Operation(summary = "Manually trigger category spending sync for a specific month")
    public ResponseEntity<Void> sync(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable int year,
            @PathVariable int month) {
        service.sync(userId, year, month);
        return ResponseEntity.noContent().build();
    }
}