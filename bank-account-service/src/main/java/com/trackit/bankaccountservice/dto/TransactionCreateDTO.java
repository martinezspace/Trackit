package com.trackit.bankaccountservice.dto;

import com.trackit.bankaccountservice.model.TransactionDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class TransactionCreateDTO {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    // Always positive - direction field determines inbound/outbound
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Direction is required")
    private TransactionDirection direction;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    // Nullable - may differ from booking date for card payments
    private LocalDate valueDate;

    // Raw bank description - nullable for manual transactions
    private String description;

    // Nullable - set by user or extracted automatically
    private String merchantName;

    // Nullable - can be categorized later
    private UUID categoryId;

    // Null for GoCardless imports - set for deduplication during import
    private String externalId;

    // True when user is creating this transaction manually
    private boolean manual = false;

    private String notes;
}