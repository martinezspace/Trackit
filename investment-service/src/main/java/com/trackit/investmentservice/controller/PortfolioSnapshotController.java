package com.trackit.investmentservice.controller;

import com.trackit.investmentservice.dto.PortfolioSnapshotCreateDTO;
import com.trackit.investmentservice.dto.PortfolioSnapshotResponseDTO;
import com.trackit.investmentservice.service.PortfolioSnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portfolio-snapshots")
@RequiredArgsConstructor
@Tag(name = "Portfolio Snapshots", description = "Historical portfolio value snapshots for charts")
public class PortfolioSnapshotController {

    private final PortfolioSnapshotService snapshotService;

    @Value("${internal.key}")
    private String internalServiceKey;

    // GET /api/portfolio-snapshots?accountId={accountId}
    @Operation(summary = "Get all snapshots for an account", description = "Returns full portfolio history oldest to newest")
    @GetMapping
    public ResponseEntity<List<PortfolioSnapshotResponseDTO>> getAllSnapshots(
            @RequestParam UUID accountId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(snapshotService.getAllSnapshots(accountId, userId));
    }

    // GET /api/portfolio-snapshots/range?accountId={accountId}&from=2024-01-01&to=2024-12-31
    @Operation(summary = "Get snapshots by date range", description = "Used for 1M, 3M, 1Y chart views")
    @GetMapping("/range")
    public ResponseEntity<List<PortfolioSnapshotResponseDTO>> getSnapshotsByDateRange(
            @RequestParam UUID accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(snapshotService.getSnapshotsByDateRange(accountId, userId, from, to));
    }

    // GET /api/portfolio-snapshots/latest?accountId={accountId}
    @Operation(summary = "Get latest snapshot", description = "Current portfolio summary for dashboard card")
    @GetMapping("/latest")
    public ResponseEntity<PortfolioSnapshotResponseDTO> getLatestSnapshot(
            @RequestParam UUID accountId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(snapshotService.getLatestSnapshot(accountId, userId));
    }

    // POST /api/portfolio-snapshots
    // Internal only — called by PriceWorker Lambda after updating all prices
    @Operation(summary = "Save portfolio snapshot", description = "Internal only — requires X-Internal-Key header")
    @PostMapping
    public ResponseEntity<PortfolioSnapshotResponseDTO> saveSnapshot(
            @RequestHeader("X-Internal-Key") String internalKey,
            @Valid @RequestBody PortfolioSnapshotCreateDTO request
    ) {
        if (!internalServiceKey.equals(internalKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(snapshotService.saveSnapshot(request));
    }
}