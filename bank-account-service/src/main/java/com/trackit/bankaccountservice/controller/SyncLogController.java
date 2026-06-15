package com.trackit.bankaccountservice.controller;

import com.trackit.bankaccountservice.dto.PagedResponseDTO;
import com.trackit.bankaccountservice.dto.SyncLogResponseDTO;
import com.trackit.bankaccountservice.model.SyncTrigger;
import com.trackit.bankaccountservice.service.SyncLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/bank-connections/{connectionId}/syncs")
@Tag(name = "Sync Log", description = "API for managing Tink sync operations")
public class SyncLogController {

    private final SyncLogService service;

    //GET /api/bank-connections/{connectionId}/syncs
    @GetMapping
    @Operation(summary = "Get paginated sync history for a connection, ordered newest first")
    public ResponseEntity<PagedResponseDTO<SyncLogResponseDTO>> getSyncLogs(
            @PathVariable UUID connectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                service.getSyncLogsForConnection(connectionId, PageRequest.of(page, size)));
    }

    //POST /api/bank-connections/{connectionId}/syncs
    @PostMapping
    @Operation(summary = "Start a new sync for a connection, returns sync log entry in RUNNING status")
    public ResponseEntity<SyncLogResponseDTO> startSync(
            @PathVariable UUID connectionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "MANUAL") SyncTrigger trigger) {
        SyncLogResponseDTO response = service.startSync(connectionId, userId, trigger);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //PATCH /api/bank-connections/{connectionId}/syncs/{syncLogId}/complete
    @PatchMapping("/{syncLogId}/complete")
    @Operation(summary = "Mark sync as completed, called by sync logic when Tink import finishes successfully")
    public ResponseEntity<SyncLogResponseDTO> completeSyncLog(
            @PathVariable UUID connectionId,
            @PathVariable UUID syncLogId,
            @RequestParam(defaultValue = "0") int fetched,
            @RequestParam(defaultValue = "0") int newCount,
            @RequestParam(defaultValue = "0") int suggested) {
        return ResponseEntity.ok(service.completeSyncLog(syncLogId, fetched, newCount, suggested));
    }

    //PATCH /api/bank-connections/{connectionId}/syncs/{syncLogId}/fail
    @PatchMapping("/{syncLogId}/fail")
    @Operation(summary = "Mark sync as failed, called by sync logic when Tink import fails")
    public ResponseEntity<SyncLogResponseDTO> failSyncLog(
            @PathVariable UUID connectionId,
            @PathVariable UUID syncLogId,
            @RequestParam String error) {
        return ResponseEntity.ok(service.failSyncLog(syncLogId, error));
    }
}