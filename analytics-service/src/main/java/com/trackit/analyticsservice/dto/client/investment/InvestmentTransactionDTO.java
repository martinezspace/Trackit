package com.trackit.analyticsservice.dto.client.investment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvestmentTransactionDTO {

    private String id;
    private String accountId;
    private String transactionType;  // BUY, SELL, DIVIDEND
    private String amount;
    private String currency;
    private String transactionDate;
    private boolean cancelled;
}