package com.trackit.investmentservice.controller;

import com.trackit.investmentservice.dto.InvestmentAccountCreateDTO;
import com.trackit.investmentservice.dto.InvestmentAccountResponseDTO;
import com.trackit.investmentservice.dto.InvestmentAccountUpdateDTO;
import com.trackit.investmentservice.service.InvestmentAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/investment-accounts")
@RequiredArgsConstructor
public class InvestmentAccountController {

    private final InvestmentAccountService investmentAccountService;

    //GET /api/investment-accounts
    @GetMapping
    public ResponseEntity<List<InvestmentAccountResponseDTO>> getAllAccounts(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(investmentAccountService.getAllAccountsForUser(userId));
    }

    //GET /api/investment-accounts/{id}
    @GetMapping("/{id}")
    public ResponseEntity<InvestmentAccountResponseDTO> getAccountById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(investmentAccountService.getAccountById(id, userId));
    }

    //POST /api/investment-accounts
    @PostMapping
    public ResponseEntity<InvestmentAccountResponseDTO> createAccount(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody InvestmentAccountCreateDTO request
            ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(investmentAccountService.createAccount(userId, request));
    }

    //PATCH /api/investment-accounts/{id}
    @PatchMapping("/{id}")
    public ResponseEntity<InvestmentAccountResponseDTO> updateAccount(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody InvestmentAccountUpdateDTO request
            ) {
        return ResponseEntity.ok(investmentAccountService.updateAccount(id, userId, request));
    }

    //DELETE /api/investment-accounts/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactiveAccount(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        investmentAccountService.deactiveAccount(id, userId);
        return ResponseEntity.noContent().build();
    }
}
