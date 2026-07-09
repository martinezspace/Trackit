package com.trackit.analyticsservice.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CashflowSummaryResponseDTO {

    private String id;
    private String userId;
    private Integer periodYear;
    private Integer periodMonth;
    private String totalIncome;
    private String totalExpenses;
    private String netCashflow;
    private Integer transactionCount;
    private String currency;
    private String updatedAt;
}