package com.trackit.investmentservice.controller;

import com.trackit.investmentservice.dto.InstrumentCreateDTO;
import com.trackit.investmentservice.dto.InstrumentResponseDTO;
import com.trackit.investmentservice.service.InstrumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/instruments")
@RequiredArgsConstructor
@Tag(name = "Instruments", description = "Reference data for financial instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    //Injected from application.yml - override with env in producation
    @Value("${internal.key}")
    private String internalServiceKey;

    //GET /api/instruments/{id}
    @Operation(summary = "Get instrument by ID", description = "Used internally when holdings or transactions need instrument details")
    @GetMapping("/{id}")
    public ResponseEntity<InstrumentResponseDTO> getById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(instrumentService.getById(id));
    }

    //GET /api/instruments/isin/{isin}
    @Operation(summary = "Get instrument by ISIN", description = "Primary lookup for CSV import and frontend instrument search")
    @GetMapping("/isin/{isin}")
    public ResponseEntity<InstrumentResponseDTO> getByIsin(
            @PathVariable String isin
    ) {
        return ResponseEntity.ok(instrumentService.getByIsin(isin));
    }

    //GET /api/instruments/tickers
    @Operation(summary = "Get all tickers")
    @GetMapping("/tickers")
    public ResponseEntity<List<String>> getAllTickers(
            @RequestHeader("X-Internal-Key") String internalKey) {
        if (!internalServiceKey.equals(internalKey)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(instrumentService.getAllTickers());
    }

    //GET /api/instruments/tickers/active
    @Operation(summary = "Get tickers with active holdings - internal use by PriceWorker")
    @GetMapping("/tickers/active")
    public ResponseEntity<List<String>> getActiveHoldingTickers(
            @RequestHeader("X-Internal-Key") String internalKey) {
        if (!internalServiceKey.equals(internalKey)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(instrumentService.getActiveHoldingTickers());
    }

    //POST /api/instruments
    @Operation(summary = "Create instrument", description = "Internal only - called during CSV import")
    @PostMapping
    public ResponseEntity<InstrumentResponseDTO> createInstrument(
            @RequestHeader("X-Internal-Key") String internalKey,
            @Valid @RequestBody InstrumentCreateDTO request
            ) {
        if (!internalServiceKey.equals(internalKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(instrumentService.createInstrument(request));
    }
}
