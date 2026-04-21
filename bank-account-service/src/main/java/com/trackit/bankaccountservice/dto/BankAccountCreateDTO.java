package com.trackit.bankaccountservice.dto;

import com.trackit.bankaccountservice.model.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class BankAccountCreateDTO {

    @NotNull(message = "Connection ID is required")
    private UUID connectionId;

    @NotBlank(message = "External account ID is required")
    private String externalAccountId;

    //Nullable - not all accounts have an IVAN e.g savings
    @Size(max = 34, message = "IBAN must be 34 characters or less")
    private String iban;

    //Nullable - user-defined label, falls back to name in mapper if not set
    @Size(max = 100, message = "Display name must be 100 characters or less")
    private String displayName;

    @NotBlank(message = "Account name is required")
    @Size(max = 255, message = "Account name must be 255 characters or less")
    private String name;

    //Nullable - defaults to PLN in mapper if not provided
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code e.g. PLN, EUR")
    private String currency;

    //Nullable - not all providers return account type
    private AccountType accountType;
}
