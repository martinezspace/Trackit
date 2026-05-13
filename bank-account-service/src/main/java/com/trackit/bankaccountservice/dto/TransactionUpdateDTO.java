package com.trackit.bankaccountservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TransactionUpdateDTO {

    // All fields nullable - only provided fields are applied in the service

    // User can reassign the category - sets categorizationStatus to MANUAL
    private UUID categoryId;

    // User can correct the merchant name if bank description was unclear
    private String merchantName;

    private String notes;
}