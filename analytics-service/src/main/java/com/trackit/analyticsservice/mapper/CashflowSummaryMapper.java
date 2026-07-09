package com.trackit.analyticsservice.mapper;

import com.trackit.analyticsservice.dto.response.CashflowSummaryResponseDTO;
import com.trackit.analyticsservice.model.CashflowSummary;
import org.springframework.stereotype.Component;

@Component
public class CashflowSummaryMapper {

    public CashflowSummaryResponseDTO toResponseDTO(CashflowSummary summary) {
        CashflowSummaryResponseDTO response = new CashflowSummaryResponseDTO();
        response.setId(summary.getId().toString());
        response.setUserId(summary.getUserId().toString());
        response.setPeriodYear(summary.getPeriodYear());
        response.setPeriodMonth(summary.getPeriodMonth());
        response.setTotalIncome(summary.getTotalIncome() != null
                ? summary.getTotalIncome().toPlainString() : null);
        response.setTotalExpenses(summary.getTotalExpenses() != null
                ? summary.getTotalExpenses().toPlainString() : null);
        response.setNetCashflow(summary.getNetCashflow() != null
                ? summary.getNetCashflow().toPlainString() : null);
        response.setTransactionCount(summary.getTransactionCount());
        response.setCurrency(summary.getCurrency());
        response.setUpdatedAt(summary.getUpdatedAt() != null
                ? summary.getUpdatedAt().toString() : null);
        return response;
    }
}