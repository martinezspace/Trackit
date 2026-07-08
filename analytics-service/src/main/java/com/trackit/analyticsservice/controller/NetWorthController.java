package com.trackit.analyticsservice.controller;

import com.trackit.analyticsservice.dto.response.NetWorthSnapshotResponseDTO;
import com.trackit.analyticsservice.service.NetWorthSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics/net-worth")
@RequiredArgsConstructor
@Tag(name = "Net Worth", description = "Net worth snapshots aggregated from bank and investment services")
public class NetWorthController {

    private final NetWorthSyncService service;

    // GET /api/analytics/net-worth
    @GetMapping
    @Operation(summary = "Get all net worth snapshots for user, ordered oldest to newest")
    public ResponseEntity<List<NetWorthSnapshotResponseDTO>> getAll(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(service.getAll(userId));
    }

    // GET /api/analytics/net-worth/latest
    @GetMapping("/latest")
    @Operation(summary = "Get most recent net worth snapshot")
    public ResponseEntity<NetWorthSnapshotResponseDTO> getLatest(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(service.getLatest(userId));
    }

    // GET /api/analytics/net-worth/range?from=2024-01-01&to=2024-12-31
    @GetMapping("/range")
    @Operation(summary = "Get net worth snapshots between two dates, used for chart range views")
    public ResponseEntity<List<NetWorthSnapshotResponseDTO>> getRange(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.getRange(userId, from, to));
    }

    // POST /api/analytics/net-worth/sync
    @PostMapping("/sync")
    @Operation(summary = "Manually trigger net worth sync for user")
    public ResponseEntity<Void> sync(@RequestHeader("X-User-Id") UUID userId) {
        service.sync(userId);
        return ResponseEntity.noContent().build();
    }
}