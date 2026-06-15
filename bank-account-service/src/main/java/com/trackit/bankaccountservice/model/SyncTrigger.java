package com.trackit.bankaccountservice.model;

public enum SyncTrigger {
    MANUAL,    // initiated by user clicking sync in the UI
    SCHEDULED, // initiated by a scheduled background job
    WEBHOOK    // initiated by a Tink webhook event
}