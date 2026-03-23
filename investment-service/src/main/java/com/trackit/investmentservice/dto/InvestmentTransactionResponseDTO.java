package com.trackit.investmentservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvestmentTransactionResponseDTO {

    private String id;
    private String accountId;

    //Instrument details embedded - avoids second API call from frotnend
    private String instrumentId;
    private String instrumentName;
    private String instrumentTicker;
    private String isin;

    private String externalId;
    private String transactionType;

    private String quantity;
    private String price;
    private String amount;
    private String currency;

    private String transactionDate;
    private boolean cancelled;
    private String createdAt;
}
