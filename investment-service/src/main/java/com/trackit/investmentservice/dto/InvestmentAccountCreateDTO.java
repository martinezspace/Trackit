package com.trackit.investmentservice.dto;

import com.trackit.investmentservice.model.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvestmentAccountCreateDTO {

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotBlank(message = "Broker name is required")
    @Size(max = 100, message = "Broker name must be 100 characters or less")
    private String brokerName;

    //Nullable - user defined, not required on creation
    @Size(max = 100, message = "Display name must be 100 characters or less")
    private String displayName;

    //Nullable - user may not have it at hand
    @Size(max = 100, message = "Account number must be 100 characters or less")
    private String accountNumber;

    //Nullable - defaults to PLN in service if not provided
    @Pattern(regexp = "^[A-Z]{3}", message = "Currency must be a valid 3-letter ISO code, e.g. PLN, USD, EUR")
    private String currency;

    //Nullable - free text
    private String notes;
}
