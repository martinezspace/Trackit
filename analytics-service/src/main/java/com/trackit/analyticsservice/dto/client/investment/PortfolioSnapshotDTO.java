package com.trackit.analyticsservice.dto.client.investment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PortfolioSnapshotDTO {

    private String id;
    private String accountId;
    private String snapshotDate;
    private String totalValue;
    private String currency;
}