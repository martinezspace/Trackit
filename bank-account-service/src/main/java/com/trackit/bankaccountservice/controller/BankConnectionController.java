package com.trackit.bankaccountservice.controller;

import com.trackit.bankaccountservice.dto.BankConnectionCreateDTO;
import com.trackit.bankaccountservice.dto.BankConnectionResponseDTO;
import com.trackit.bankaccountservice.dto.BankConnectionUpdateDTO;
import com.trackit.bankaccountservice.service.BankConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/bank-connections")
@Tag(name = "Bank Connection", description = "API for managing bank connections")
public class BankConnectionController {

    private final BankConnectionService bankConnectionService;

    //GET /api/bank-connections
    @GetMapping
    @Operation(summary = "Get all connections for authenticated user")
    public ResponseEntity<List<BankConnectionResponseDTO>> getAllConnections(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(bankConnectionService.getAllConnectionsForUser(userId));
    }

    //GET /api/bank-connections/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Single connection if it belongs to the user")
    public ResponseEntity<BankConnectionResponseDTO> getConnection(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(bankConnectionService.getConnectionById(id, userId));
    }

    //POST /api/bank-connections
    @PostMapping
    @Operation(summary = "Create connection, status defaults to PENDING")
    public ResponseEntity<BankConnectionResponseDTO> createConnection(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody BankConnectionCreateDTO request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bankConnectionService.createConnection(userId, request));
    }

    //PATCH /api/bank-connections/{id}
    @PatchMapping("/{id}")
    @Operation(summary = "Update an existing Bank Connection")
    public ResponseEntity<BankConnectionResponseDTO> updateConnection(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody BankConnectionUpdateDTO request) {
        return ResponseEntity.ok(bankConnectionService.updateConnection(id, userId, request));
    }

    //DELETE /api/bank-connections/{id}
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete specific Bank Connection")
    public ResponseEntity<Void> deleteConnection(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        bankConnectionService.deleteConnection(id, userId);
        return ResponseEntity.noContent().build();
    }
}
