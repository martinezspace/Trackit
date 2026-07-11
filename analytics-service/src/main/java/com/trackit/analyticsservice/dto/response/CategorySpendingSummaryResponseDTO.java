package com.trackit.analyticsservice.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategorySpendingSummaryResponseDTO {

    private String id;
    private String userId;
    private String categoryId;
    private String categoryName;
    private String categoryColor;
    private Integer periodYear;
    private Integer periodMonth;
    private String totalAmount;
    private Integer transactionCount;
    private String currency;
    private String updatedAt;
}