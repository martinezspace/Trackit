package com.trackit.investmentservice.mapper;

import com.trackit.investmentservice.dto.PortfolioSnapshotCreateDTO;
import com.trackit.investmentservice.dto.PortfolioSnapshotResponseDTO;
import com.trackit.investmentservice.model.InvestmentAccount;
import com.trackit.investmentservice.model.PortfolioSnapshot;
import org.hibernate.boot.model.internal.CreateKeySecondPass;
import org.springframework.stereotype.Component;

@Component
public class PortfolioSnapshotMapper {

    //CreateDTO + InvestmentAccount => Entity
    //Account passed separately
    //DTO has accountId, eneity needs full InvestmentAccount for ManyToOne
    public PortfolioSnapshot toEntity(PortfolioSnapshotCreateDTO request, InvestmentAccount account) {
        PortfolioSnapshot snapshot = new PortfolioSnapshot();
        snapshot.setAccount(account);
        snapshot.setSnapshotDate(request.getSnapshotDate());
        snapshot.setTotalValue(request.getTotalValue());
        snapshot.setTotalInvested(request.getTotalInvested());
        snapshot.setTotalGainLoss(request.getTotalGainLoss());
        snapshot.setGainLossPct(request.getGainLossPct());
        snapshot.setCurrency(request.getCurrency());
        return snapshot;
    }

    //Entity => ResponseDTO
    public PortfolioSnapshotResponseDTO toResponseDTO(PortfolioSnapshot snapshot) {
        PortfolioSnapshotResponseDTO response = new PortfolioSnapshotResponseDTO();
        response.setId(snapshot.getId().toString());
        response.setAccountId(snapshot.getAccount().getId().toString());
        response.setSnapshotDate(snapshot.getSnapshotDate().toString());
        response.setTotalValue(snapshot.getTotalValue().toPlainString());
        response.setTotalInvested(snapshot.getTotalInvested().toPlainString());
        response.setTotalGainLoss(snapshot.getTotalGainLoss().toPlainString());
        response.setGainLossPct(snapshot.getGainLossPct().toPlainString());
        response.setCurrency(snapshot.getCurrency());
        response.setCreatedAt(snapshot.getCreatedAt() != null
                ? snapshot.getCreatedAt().toString()
                : null);
        return response;
    }
}
