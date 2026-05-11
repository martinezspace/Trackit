package com.trackit.bankaccountservice.dto;

import com.trackit.bankaccountservice.model.RuleMatchField;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class TransactionRuleUpdateDTO {

    // All fields nullable - only provided fields are applied in the service

    @Size(max = 100, message = "Rule name must be 100 characters or less")
    private String name;

    private RuleMatchField matchField;

    @Size(max = 255, message = "Match pattern must be 255 characters or less")
    private String matchPattern;

    private BigDecimal amountMin;
    private BigDecimal amountMax;

    // Nullable - only provided if user wants to reassign the target category
    private UUID categoryId;

    // Integer not int so null means "no change" - int would default to 0
    private Integer priority;
}