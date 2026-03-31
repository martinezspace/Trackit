package com.trackit.investmentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

//Internal DTO not exposed to users via controller
//Used by PriceWorker Lambda to create daily portfolio snapshots
//after all holding prices have been updated
//Uses proper types since constructed programitaclly
@Getter
@Setter
public class PortfolioSnapshotCreateDTO {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotNull(message = "Snapshot date is required")
    private LocalDate snapshotDate;

    @NotNull(message = "Total value is required")
    private BigDecimal totalValue;

    @NotNull(message = "Total invested is required")
    private BigDecimal totalInvested;

    @NotNull(message = "Total gain/loss is required")
    private BigDecimal totalGainLoss;

    @NotNull(message = "Gain/loss percentage is required")
    private BigDecimal gainLossPct;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
    private String currency;
}
