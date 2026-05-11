package com.trackit.bankaccountservice.dto;

import com.trackit.bankaccountservice.model.RuleMatchField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class TransactionRuleCreateDTO {

    @NotBlank(message = "Rule name is required")
    @Size(max = 100, message = "Rule name must be 100 characters or less")
    private String name;

    @NotNull(message = "Match field is required")
    private RuleMatchField matchField;

    @NotBlank(message = "Match pattern is required")
    @Size(max = 255, message = "Match pattern must be 255 characters or less")
    private String matchPattern;

    // Nullable - no lower bound on amount if omitted
    private BigDecimal amountMin;

    // Nullable - no upper bound on amount if omitted
    private BigDecimal amountMax;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    // Higher number = evaluated first when multiple rules could match
    private int priority = 0;
}