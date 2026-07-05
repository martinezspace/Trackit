package com.trackit.analyticsservice.mapper;

import com.trackit.analyticsservice.dto.response.NetWorthSnapshotResponseDTO;
import com.trackit.analyticsservice.model.NetWorthSnapshot;
import org.springframework.stereotype.Component;

@Component
public class NetWorthSnapshotMapper {

    public NetWorthSnapshotResponseDTO toResponseDTO(NetWorthSnapshot snapshot) {
        NetWorthSnapshotResponseDTO response = new NetWorthSnapshotResponseDTO();
        response.setId(snapshot.getId().toString());
        response.setUserId(snapshot.getUserId().toString());
        response.setSnapshotDate(snapshot.getSnapshotDate().toString());
        response.setBankBalanceTotal(snapshot.getBankBalanceTotal() != null
                ? snapshot.getBankBalanceTotal().toPlainString() : null);
        response.setInvestmentValueTotal(snapshot.getInvestmentValueTotal() != null
                ? snapshot.getInvestmentValueTotal().toPlainString() : null);
        response.setNetWorth(snapshot.getNetWorth() != null
                ? snapshot.getNetWorth().toPlainString() : null);
        response.setCurrency(snapshot.getCurrency());
        response.setCreatedAt(snapshot.getCreatedAt() != null
                ? snapshot.getCreatedAt().toString() : null);
        return response;
    }
}