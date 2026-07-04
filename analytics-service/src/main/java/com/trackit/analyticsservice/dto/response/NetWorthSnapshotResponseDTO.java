package com.trackit.analyticsservice.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NetWorthSnapshotResponseDTO {

    private String id;
    private String userId;
    private String snapshotDate;
    private String bankBalanceTotal;
    private String investmentValueTotal;
    private String netWorth;
    private String currency;
    private String createdAt;
}