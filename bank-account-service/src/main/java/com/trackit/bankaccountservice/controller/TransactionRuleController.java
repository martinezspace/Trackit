package com.trackit.bankaccountservice.controller;

import com.trackit.bankaccountservice.dto.TransactionRuleCreateDTO;
import com.trackit.bankaccountservice.dto.TransactionRuleResponseDTO;
import com.trackit.bankaccountservice.dto.TransactionRuleUpdateDTO;
import com.trackit.bankaccountservice.service.TransactionRuleService;
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
@RequestMapping("/api/transaction-rules")
@Tag(name = "Transaction Rule", description = "API for managing auto-categorization rules")
public class TransactionRuleController {

    private final TransactionRuleService service;

    //GET /api/transaction-rules
    @GetMapping
    @Operation(summary = "Get all active rules for authenticated user, ordered by priority descending")
    public ResponseEntity<List<TransactionRuleResponseDTO>> getAllRules(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(service.getAllRulesForUser(userId));
    }

    //GET /api/transaction-rules/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Get single rule if it belongs to the authenticated user")
    public ResponseEntity<TransactionRuleResponseDTO> getRule(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(service.getRuleById(id, userId));
    }

    //POST /api/transaction-rules
    @PostMapping
    @Operation(summary = "Create new auto-categorization rule")
    public ResponseEntity<TransactionRuleResponseDTO> createRule(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody TransactionRuleCreateDTO request) {
        TransactionRuleResponseDTO response = service.createRule(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //PATCH /api/transaction-rules/{id}
    @PatchMapping("/{id}")
    @Operation(summary = "Update existing rule, only provided fields are applied")
    public ResponseEntity<TransactionRuleResponseDTO> updateRule(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody TransactionRuleUpdateDTO request) {
        return ResponseEntity.ok(service.updateRule(id, userId, request));
    }

    //DELETE /api/transaction-rules/{id}
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete rule, sets is_active=false so history is preserved but rule no longer runs")
    public ResponseEntity<Void> deleteRule(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        service.deleteRule(id, userId);
        return ResponseEntity.noContent().build();
    }
}