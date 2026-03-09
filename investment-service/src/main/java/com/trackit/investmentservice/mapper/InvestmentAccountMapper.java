package com.trackit.investmentservice.mapper;

import com.trackit.investmentservice.dto.InvestmentAccountCreateDTO;
import com.trackit.investmentservice.dto.InvestmentAccountResponseDTO;
import com.trackit.investmentservice.dto.InvestmentAccountUpdateDTO;
import com.trackit.investmentservice.model.InvestmentAccount;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InvestmentAccountMapper {

    // Entity => ResponseDTO
    public InvestmentAccountResponseDTO toResponseDTO(InvestmentAccount account) {
        InvestmentAccountResponseDTO response = new InvestmentAccountResponseDTO();
        response.setId(account.getId().toString());
        response.setAccountType(account.getAccountType().name());
        response.setBrokerName(account.getBrokerName());
        response.setDisplayName(resolveDisplayName(account));
        response.setAccountNumber(account.getAccountNumber());
        response.setCurrency(account.getCurrency());
        response.setActive(account.isActive());
        response.setNotes(account.getNotes());
        response.setCreatedAt(account.getCreatedAt() != null
                ? account.getCreatedAt().toString()
                : null);
        return response;
    }

    //CreateDTO => Entity
    public InvestmentAccount toEntity(InvestmentAccountCreateDTO request, UUID userId) {
        InvestmentAccount account = new InvestmentAccount();
        account.setUserId(userId);
        account.setAccountType(request.getAccountType());
        account.setBrokerName(request.getBrokerName());
        account.setDisplayName(request.getDisplayName());
        account.setAccountNumber(request.getAccountNumber());
        account.setCurrency(request.getCurrency() != null ? request.getCurrency() : "PLN");
        account.setNotes(request.getNotes());
        account.setActive(true);
        return account;
    }

    //UpdateDTO => Entity - applies only non-full fields onto existing entity
    public InvestmentAccount applyUpdate(InvestmentAccount existing, InvestmentAccountUpdateDTO request) {
        if (request.getDisplayName() != null) {
            existing.setDisplayName(request.getDisplayName());
        }
        if (request.getAccountNumber() != null) {
            existing.setAccountNumber(request.getAccountNumber());
        }
        if (request.getNotes() != null) {
            existing.setNotes(request.getNotes());
        }
        return existing;
    }

    //Helper
    private String resolveDisplayName(InvestmentAccount account) {
        if (account.getDisplayName() != null && !account.getDisplayName().isBlank()) {
            return account.getDisplayName();
        }
        return account.getBrokerName() + " " + account.getAccountType().name();
    }
}
