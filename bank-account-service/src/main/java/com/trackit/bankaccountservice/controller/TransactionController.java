package com.trackit.bankaccountservice.controller;

import com.trackit.bankaccountservice.dto.PagedResponseDTO;
import com.trackit.bankaccountservice.dto.TransactionCreateDTO;
import com.trackit.bankaccountservice.dto.TransactionResponseDTO;
import com.trackit.bankaccountservice.dto.TransactionUpdateDTO;
import com.trackit.bankaccountservice.model.TransactionDirection;
import com.trackit.bankaccountservice.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction", description = "API for managing bank transactions")
public class TransactionController {

    private final TransactionService service;

    //GET /api/transactions
    @GetMapping
    @Operation(summary = "Get paginated and filtered transactions for authenticated user, ordered by bookingDate descending")
    public ResponseEntity<PagedResponseDTO<TransactionResponseDTO>> getTransactions(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) TransactionDirection direction,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        // Cap page size to prevent accidentally large queries
        PageRequest pageable = PageRequest.of(page, Math.min(size, 200),
                Sort.by("bookingDate").descending());

        return ResponseEntity.ok(service.getTransactions(
                userId, accountId, categoryId, direction,
                from, to, minAmount, maxAmount, pageable));
    }

    //GET /api/transactions/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Get single transaction if it belongs to the authenticated user")
    public ResponseEntity<TransactionResponseDTO> getTransaction(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(service.getTransactionById(id, userId));
    }

    //POST /api/transactions
    @PostMapping
    @Operation(summary = "Create new transaction, idempotent on accountId and externalId combination")
    public ResponseEntity<TransactionResponseDTO> createTransaction(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody TransactionCreateDTO request) {
        TransactionResponseDTO response = service.createTransaction(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //PATCH /api/transactions/{id}
    @PatchMapping("/{id}")
    @Operation(summary = "Update transaction, only provided fields are applied, setting categoryId marks it as MANUAL categorization")
    public ResponseEntity<TransactionResponseDTO> updateTransaction(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody TransactionUpdateDTO request) {
        return ResponseEntity.ok(service.updateTransaction(id, userId, request));
    }

    //DELETE /api/transactions/{id}
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete transaction, returns 400 if transaction was imported and not manually created")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        service.deleteTransaction(id, userId);
        return ResponseEntity.noContent().build();
    }

    //POST /api/transactions/reapply-rules
    @PostMapping("/reapply-rules")
    @Operation(summary = "Re-run active rules against all uncategorized transactions, returns count of transactions categorized")
    public ResponseEntity<Integer> reapplyRules(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(service.reapplyRules(userId));
    }
}