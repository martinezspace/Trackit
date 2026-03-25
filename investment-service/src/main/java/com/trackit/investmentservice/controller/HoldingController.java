package com.trackit.investmentservice.controller;

import com.trackit.investmentservice.dto.HoldingResponseDTO;
import com.trackit.investmentservice.service.HoldingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/holdings")
@RequiredArgsConstructor
@Tag(name = "Holdings", description = "View current investment positions")
public class HoldingController {

    private final HoldingService holdingService;

    //GET /api/holdings?accountId={accountId}
    @Operation(summary = "Get all holdings for an account", description = "Returns current portfolio positions")
    @GetMapping
    public ResponseEntity<List<HoldingResponseDTO>> getAllHoldings(
            @RequestParam UUID accountId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(holdingService.getAllHoldingsForAccount(accountId, userId));
    }

    //GET /api/holdings/instrument/{instrumentId}?accountId={accountId}
    @Operation(summary = "Get holding by instrument", description = "Returns position for a specific instrument in an account")
    @GetMapping("/instrument/{instrumentId}")
    public ResponseEntity<HoldingResponseDTO> getHoldingByInstrument(
            @PathVariable UUID instrumentId,
            @RequestParam UUID accountId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(holdingService.getHoldingByInstrument(accountId, instrumentId, userId));
    }
}
