package com.trackit.investmentservice.controller;

import com.trackit.investmentservice.dto.InvestmentTransactionResponseDTO;
import com.trackit.investmentservice.service.InvestmentTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/transactions")
@RequiredArgsConstructor
@Tag(name = "Investment Transactions", description = "View and manage investment transactions")
public class InvestmentTransactionController {

    private final InvestmentTransactionService transactionService;

    //GET /api/transactions?accountId={accountId}
    @Operation(summary = "Get all transactions for an account", description = "Returns all active transactions excluding cancelled ones")
    @GetMapping
    public ResponseEntity<List<InvestmentTransactionResponseDTO>> getAllTransactions(
            @RequestParam UUID accountId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(transactionService.getAllTransactionsForAccount(accountId, userId));
    }

    //GET /api/transactions?accountId={accountId}&instrumentId={instrumentId}
    @Operation(summary = "Get transactions by instrument", description = "Returns transactions for a specific instrument in an account")
    @GetMapping("/instrument/{instrumentId}")
    public ResponseEntity<List<InvestmentTransactionResponseDTO>> getTransactionsByInstrument(
            @PathVariable UUID instrumentId,
            @RequestParam UUID accountId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(transactionService.getTransactionsByInstrument(accountId, instrumentId, userId));
    }

    //GET /api/transactions/{id}?accountId={accountId}
    @Operation(summary = "Get transaction by ID")
    @GetMapping("/{id}")
    public ResponseEntity<InvestmentTransactionResponseDTO> getTransactionById(
            @PathVariable UUID id,
            @RequestParam UUID accountId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(transactionService.getTransactionById(id, accountId, userId));
    }

    //PATCH /api/transactions/{id}/cancel?accountId={accountId}
    @Operation(summary = "Cancel a transaction", description = "Soft cancels a transaction - does not delete it")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<InvestmentTransactionResponseDTO> cancelTransaction(
            @PathVariable UUID id,
            @RequestParam UUID accountId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(transactionService.cancelTransaction(id, accountId, userId));
    }
}
