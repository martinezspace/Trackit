package com.trackit.analyticsservice.controller;

import com.trackit.analyticsservice.dto.response.AssetAllocationResponseDTO;
import com.trackit.analyticsservice.service.AssetAllocationSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics/asset-allocation")
@RequiredArgsConstructor
@Tag(name = "Asset Allocation", description = "Current portfolio breakdown by investment account")
public class AssetAllocationController {

    private final AssetAllocationSyncService service;

    // GET /api/analytics/asset-allocation
    @GetMapping
    @Operation(summary = "Get current asset allocation for user, ordered by weight descending")
    public ResponseEntity<List<AssetAllocationResponseDTO>> getAll(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(service.getAll(userId));
    }

    // POST /api/analytics/asset-allocation/sync
    @PostMapping("/sync")
    @Operation(summary = "Manually trigger asset allocation sync")
    public ResponseEntity<Void> sync(@RequestHeader("X-User-Id") UUID userId) {
        service.sync(userId);
        return ResponseEntity.noContent().build();
    }
}