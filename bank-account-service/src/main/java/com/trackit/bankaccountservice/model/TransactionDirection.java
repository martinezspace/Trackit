package com.trackit.bankaccountservice.model;

public enum TransactionDirection {
    INBOUND,   // money received - salary, transfers in, refunds
    OUTBOUND   // money spent - purchases, bills, transfers out
}