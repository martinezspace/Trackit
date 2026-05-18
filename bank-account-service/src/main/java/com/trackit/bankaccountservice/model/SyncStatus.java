package com.trackit.bankaccountservice.model;

public enum SyncStatus {
    RUNNING,   // sync is currently in progress
    COMPLETED, // sync completed without errors
    FAILED     // sync encountered an error - see error_message for details
}