package com.trackit.bankaccountservice.model;

public enum CategorizationStatus {
    UNCATEGORIZED, // no category assigned yet
    AUTO,          // assigned by rule engine automatically
    MANUAL,        // set explicitly by user - never overwritten by rule engine
    SUGGESTED      // rule engine found low-confidence match - awaiting user confirmation
}