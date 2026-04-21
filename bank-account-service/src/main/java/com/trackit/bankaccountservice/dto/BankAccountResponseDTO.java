package com.trackit.bankaccountservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankAccountResponseDTO {

    private String id;
    private String connectionId;
    private String userId;
    private String externalAccountId;
    private String iban;
    private String displayName;     //resolved in mapper - falls back to name if null
    private String name;
    private String currency;
    private String accountType;     //null if not provided by bank
    private String currentBalance;  //null if not yet fetched
    private String availableBalance;//null if not yet fetched
    private String balanceUpdatedAt;//null if balance never fetched
    private boolean active;
    private String createdAt;
    private String updatedAt;
}
