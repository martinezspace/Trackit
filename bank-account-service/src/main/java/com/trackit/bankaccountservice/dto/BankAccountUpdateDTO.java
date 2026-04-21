package com.trackit.bankaccountservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankAccountUpdateDTO {

    //All fields nullable - user sends only what they want to change

    //User can rename their account label in the UI
    @Size(max = 100, message = "Display name must be 100 characters or less")
    private String displayName;
}
