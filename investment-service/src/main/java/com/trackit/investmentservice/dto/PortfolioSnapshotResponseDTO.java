package com.trackit.investmentservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PortfolioSnapshotResponseDTO {

    private String id;
    private String accountId;
    private String snapshotDate;

    private String totalValue;
    private String totalInvested;
    private String totalGainLoss;
    private String gainLossPct;

    private String currency;
    private String createdAt;
}
