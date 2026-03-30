package com.trackit.investmentservice.controller;

import com.trackit.investmentservice.dto.PriceHistoryCreateDTO;
import com.trackit.investmentservice.dto.PriceHistoryResponseDTO;
import com.trackit.investmentservice.service.PriceHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/price-history")
@RequiredArgsConstructor
@Tag(name = "Price History", description = "Instrument price history for charts and current valuations")
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    @Value("${internal.key}")
    private String internalServiceKey;

    //GET /api/price-history/{instrumentId}/latest
    @Operation(summary = "Get latest price for an instrument")
    @GetMapping("/{instrumentId}/latest")
    public ResponseEntity<PriceHistoryResponseDTO> getLatestPrice(
            @PathVariable UUID instrumentId
    ) {
        return ResponseEntity.ok(priceHistoryService.getLatestPrice(instrumentId));
    }

    //GET /api/price-history/{instrumentId}?from=2024-01-01&to=2024-12-31
    @Operation(summary = "Get price history for a date range", description = "Used for portfolio charts")
    @GetMapping("/{instrumentId}")
    public ResponseEntity<List<PriceHistoryResponseDTO>> getPriceHistory(
            @PathVariable UUID instrumentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
            ) {
        return ResponseEntity.ok(priceHistoryService.getPriceHistory(instrumentId, from, to));
    }

    //POST /api/price-history
    //Internal-only called by PriceWorker lambda after fetching prices from Alpha
    @Operation(summary = "Save new price", description = "Internal only - requires X-Internal-Key header")
    @PostMapping
    public ResponseEntity<PriceHistoryResponseDTO> savePrice(
            @RequestHeader("X-Internal-Key") String internalKey,
            @Valid @RequestBody PriceHistoryCreateDTO request
            ) {
        if (!internalServiceKey.equals(internalKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(priceHistoryService.savePrice(request));
    }
}
