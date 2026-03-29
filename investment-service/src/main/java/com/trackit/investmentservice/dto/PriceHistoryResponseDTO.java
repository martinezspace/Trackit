package com.trackit.investmentservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PriceHistoryResponseDTO {

    private String id;
    private String instrumentId;
    private String instrumentTicker;
    private String instrumentName;
    private String priceDate;
    private String closePrice;
    private String currency;
    private String source;
    private String createdAt;
}
