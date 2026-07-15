package com.trackit.analyticsservice.mapper;

import com.trackit.analyticsservice.dto.response.MonthlyInvestmentSummaryResponseDTO;
import com.trackit.analyticsservice.model.MonthlyInvestmentSummary;
import org.springframework.stereotype.Component;

@Component
public class MonthlyInvestmentSummaryMapper {

    public MonthlyInvestmentSummaryResponseDTO toResponseDTO(MonthlyInvestmentSummary summary) {
        MonthlyInvestmentSummaryResponseDTO response = new MonthlyInvestmentSummaryResponseDTO();
        response.setId(summary.getId().toString());
        response.setUserId(summary.getUserId().toString());
        response.setAccountId(summary.getAccountId().toString());
        response.setAccountType(summary.getAccountType());
        response.setPeriodYear(summary.getPeriodYear());
        response.setPeriodMonth(summary.getPeriodMonth());
        response.setContributionsTotal(summary.getContributionsTotal() != null
                ? summary.getContributionsTotal().toPlainString() : null);
        response.setWithdrawalsTotal(summary.getWithdrawalsTotal() != null
                ? summary.getWithdrawalsTotal().toPlainString() : null);
        response.setDividendsTotal(summary.getDividendsTotal() != null
                ? summary.getDividendsTotal().toPlainString() : null);
        response.setFeesTotal(summary.getFeesTotal() != null
                ? summary.getFeesTotal().toPlainString() : null);
        response.setTaxesTotal(summary.getTaxesTotal() != null
                ? summary.getTaxesTotal().toPlainString() : null);
        response.setCurrency(summary.getCurrency());
        response.setUpdatedAt(summary.getUpdatedAt() != null
                ? summary.getUpdatedAt().toString() : null);
        return response;
    }
}