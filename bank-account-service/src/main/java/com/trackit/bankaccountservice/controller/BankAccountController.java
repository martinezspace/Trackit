package com.trackit.bankaccountservice.controller;

import com.trackit.bankaccountservice.dto.BankAccountCreateDTO;
import com.trackit.bankaccountservice.dto.BankAccountResponseDTO;
import com.trackit.bankaccountservice.dto.BankAccountUpdateDTO;
import com.trackit.bankaccountservice.service.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/bank-accounts")
@Tag(name = "Bank Account", description = "API for managing bank accounts")
public class BankAccountController {

    private final BankAccountService service;

    //GET /api/bank-accounts
    @GetMapping
    @Operation(summary = "Get all active accounts for authenticated user")
    public ResponseEntity<List<BankAccountResponseDTO>> getAllAccounts(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(service.getAllAccountsForUser(userId));
    }

    //GET /api/bank-accounts/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Get single account if it belongs to user")
    public ResponseEntity<BankAccountResponseDTO> getAccount(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(service.getAccountById(id, userId));
    }

    //POST /api/bank-accounts
    @PostMapping
    @Operation(summary = "Creates new account")
    public ResponseEntity<BankAccountResponseDTO> createAccount(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody BankAccountCreateDTO request) {
        BankAccountResponseDTO response = service.createAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //PATCH /api/bank-accounts/{id}
    @PatchMapping("/{id}")
    @Operation(summary = "Updates specific account")
    public ResponseEntity<BankAccountResponseDTO> updateAccount(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody BankAccountUpdateDTO request) {
        return ResponseEntity.ok(service.updateAccount(id, userId, request));
    }

    //PATCH /api/bank-accounts/{id}/balance
    @PatchMapping("/{id}/balance")
    @Operation(summary = "Called ny sync after fetching latest balances to update")
    public ResponseEntity<BankAccountResponseDTO> updateBalance(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam BigDecimal currrent,
            @RequestParam(required = false) BigDecimal available) {
        return ResponseEntity.ok(service.updateBalance(id, userId, currrent, available));
    }

    //DELETE /api/bank-accounts/{id}
    @DeleteMapping("/{id}")
    @Operation(summary = "Deactive specific Bank Account")
    public ResponseEntity<Void> deactiveAccount(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        service.deactivateAccount(id, userId);
        return ResponseEntity.noContent().build();
    }
}
