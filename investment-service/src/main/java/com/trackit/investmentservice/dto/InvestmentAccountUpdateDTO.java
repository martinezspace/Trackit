package com.trackit.investmentservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvestmentAccountUpdateDTO {

    // All fields nullable - user sends only what they want to change
    // Fields not sent remain unchanged in the service

    @Size(max = 100, message = "Display name must be 100 characters or less")
    private String displayName;

    @Size(max = 100, message = "Account number must be 100 characters or less")
    private String accountNumber;

    private String notes;
}
