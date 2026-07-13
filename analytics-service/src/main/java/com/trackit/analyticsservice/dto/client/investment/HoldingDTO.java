package com.trackit.analyticsservice.dto.client.investment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HoldingDTO {

    private String id;
    private String accountId;
    private String instrumentId;
    private String instrumentName;
    private String currentValue;    // null until PriceWorker runs
    private String currency;
}