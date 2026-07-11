package com.trackit.analyticsservice.mapper;

import com.trackit.analyticsservice.dto.response.CategorySpendingSummaryResponseDTO;
import com.trackit.analyticsservice.model.CategorySpendingSummary;
import org.springframework.stereotype.Component;

@Component
public class CategorySpendingSummaryMapper {

    public CategorySpendingSummaryResponseDTO toResponseDTO(CategorySpendingSummary summary) {
        CategorySpendingSummaryResponseDTO response = new CategorySpendingSummaryResponseDTO();
        response.setId(summary.getId().toString());
        response.setUserId(summary.getUserId().toString());
        response.setCategoryId(summary.getCategoryId().toString());
        response.setCategoryName(summary.getCategoryName());
        response.setCategoryColor(summary.getCategoryColor());
        response.setPeriodYear(summary.getPeriodYear());
        response.setPeriodMonth(summary.getPeriodMonth());
        response.setTotalAmount(summary.getTotalAmount() != null
                ? summary.getTotalAmount().toPlainString() : null);
        response.setTransactionCount(summary.getTransactionCount());
        response.setCurrency(summary.getCurrency());
        response.setUpdatedAt(summary.getUpdatedAt() != null
                ? summary.getUpdatedAt().toString() : null);
        return response;
    }
}