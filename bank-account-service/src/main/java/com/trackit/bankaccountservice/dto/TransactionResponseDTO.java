package com.trackit.bankaccountservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionResponseDTO {

    private String id;
    private String accountId;
    private String userId;
    private String externalId;           // null for manual transactions
    private String amount;               // always positive
    private String direction;            // INBOUND or OUTBOUND
    private String currency;
    private String bookingDate;          // yyyy-MM-dd
    private String valueDate;            // yyyy-MM-dd, null if not provided
    private String description;
    private String merchantName;
    private String categoryId;           // null if uncategorized
    private String categoryName;         // null if uncategorized - convenience field for UI
    private String categorizationStatus;
    private boolean manual;
    private String notes;
    private String createdAt;
    private String updatedAt;
}