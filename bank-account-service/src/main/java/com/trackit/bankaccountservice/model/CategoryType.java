package com.trackit.bankaccountservice.model;

public enum CategoryType {
    DEBIT,   // outgoing e.g. Food, Transport
    CREDIT,  // incoming e.g. Salary, Freelance
    BOTH     // both directions e.g. Transfers
}