package com.trackit.analyticsservice.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonthlyInvestmentSummaryResponseDTO {

    private String id;
    private String userId;
    private String accountId;
    private String accountType;
    private Integer periodYear;
    private Integer periodMonth;
    private String contributionsTotal;
    private String withdrawalsTotal;
    private String dividendsTotal;
    private String feesTotal;
    private String taxesTotal;
    private String currency;
    private String updatedAt;
}