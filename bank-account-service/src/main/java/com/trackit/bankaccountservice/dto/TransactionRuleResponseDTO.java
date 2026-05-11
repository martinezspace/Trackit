package com.trackit.bankaccountservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionRuleResponseDTO {

    private String id;
    private String userId;
    private String name;
    private String matchField;
    private String matchPattern;
    private String amountMin;    // null if no lower bound
    private String amountMax;    // null if no upper bound
    private String categoryId;
    private String categoryName; // convenience field - avoids extra lookup on frontend
    private int priority;
    private boolean active;
    private String createdAt;
    private String updatedAt;
}