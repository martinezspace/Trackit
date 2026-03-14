package com.trackit.investmentservice.controller;

import com.trackit.investmentservice.dto.ImportBatchCreateDTO;
import com.trackit.investmentservice.dto.ImportBatchResponseDTO;
import com.trackit.investmentservice.repository.ImportBatchRepository;
import com.trackit.investmentservice.service.ImportBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/import-batches")
@RequiredArgsConstructor
@Tag(name = "Import Batches", description = "Manage CSV import batches for investment accounts")
public class ImportBatchController {

    private final ImportBatchService importBatchService;

    //GET /api/import-batches?accountId={accountId}
    @Operation(summary = "Get all import batches for an account", description = "Returns import history newest first")
    @GetMapping
    public ResponseEntity<List<ImportBatchResponseDTO>> getAllBatches(
            @RequestParam UUID accountId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(importBatchService.getAllBatchesForAccount(accountId, userId));
    }

    //GET /api/import-batches/{id}?accountId={accountId}
    @Operation(summary = "Get import batch by ID", description = "Used for status polling after upload")
    @GetMapping("/{id}")
    public ResponseEntity<ImportBatchResponseDTO> getBatchById(
            @PathVariable UUID id,
            @RequestParam UUID accountId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(importBatchService.getBatchById(id, accountId, userId));
    }

    //POST /api/import-batches
    @Operation(summary = "Create import batch", description = "Registers a new CSV import - actual file upload handled separately")
    @PostMapping
    public ResponseEntity<ImportBatchResponseDTO> createBatch(
            @Valid @RequestBody ImportBatchCreateDTO request,
            @RequestHeader("X-User-Id") UUID userId
            ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(importBatchService.createBatch(request, userId));
    }

    //PATCH /api/import-batches/{id}/cancel?accountId={accountId}
    @Operation(summary = "Cancel import batch", description = "Cancels a PENDING or PROCESSING import")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ImportBatchResponseDTO> cancelBatch(
            @PathVariable UUID id,
            @RequestParam UUID accountId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(importBatchService.cancelBatch(id, accountId, userId));
    }
}
