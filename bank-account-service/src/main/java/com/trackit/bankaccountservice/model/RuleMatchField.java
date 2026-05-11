package com.trackit.bankaccountservice.model;

public enum RuleMatchField {
    DESCRIPTION, // match against raw bank description field
    MERCHANT,    // match against merchant name field
    BOTH         // match against either
}