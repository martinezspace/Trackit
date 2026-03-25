package com.trackit.investmentservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HoldingResponseDTO {

    private String id;
    private String accountId;

    //Instrument details embedded - avoids second API call from frontend
    private String instrumentId;
    private String instrumentName;
    private String instrumentTicker;
    private String isin;

    //Position data - String to avoid floating point issues in JSON
    private String quantity;
    private String avgPurchasePrice;
    private String totalInvested;

    //Price data - nullable until PriceWorker runs
    private String currentPrice;
    private String currentValue;
    private String unrealizedPnL;
    private String unrealizedPnlPct;

    private String currency;

    //Nullable - null until first price fetch
    private String lastPriceUpdate;
    private String updatedAt;
}
