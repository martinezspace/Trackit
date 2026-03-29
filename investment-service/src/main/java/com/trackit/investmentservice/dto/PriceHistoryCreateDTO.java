package com.trackit.investmentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

//Internal DTO not exposed to users via controller
//Used by PriceWorker Lambda to insert new daily prices after fetching from Alpha
//no user input no need for string parsing
@Getter
@Setter
public class PriceHistoryCreateDTO {

    @NotNull(message = "Instrument ID is required")
    private UUID instrumentId;

    @NotNull(message = "Price date is required")
    private LocalDate priceDate;

    @NotNull(message = "Close price is required")
    private BigDecimal closePrice;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
    private String currency;

    //Which price provider this came from
    @Size(max = 50)
    private String source;
}
