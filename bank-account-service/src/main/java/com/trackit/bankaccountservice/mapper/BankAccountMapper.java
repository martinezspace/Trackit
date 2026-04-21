package com.trackit.bankaccountservice.mapper;

import com.trackit.bankaccountservice.dto.BankAccountCreateDTO;
import com.trackit.bankaccountservice.dto.BankAccountResponseDTO;
import com.trackit.bankaccountservice.dto.BankAccountUpdateDTO;
import com.trackit.bankaccountservice.dto.BankConnectionCreateDTO;
import com.trackit.bankaccountservice.model.BankAccount;
import com.trackit.bankaccountservice.model.BankConnection;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class BankAccountMapper {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    //Entity => ResponseDTO
    public BankAccountResponseDTO toResponseDTO(BankAccount account) {
        BankAccountResponseDTO response = new BankAccountResponseDTO();
        response.setId(account.getId().toString());
        response.setConnectionId(account.getConnection().getId().toString());
        response.setUserId(account.getUserId().toString());
        response.setExternalAccountId(account.getExternalAccountId());
        response.setIban(account.getIban());
        response.setDisplayName(resolveDisplayName(account));
        response.setName(account.getName());
        response.setCurrency(account.getCurrency());
        response.setAccountType(account.getAccountType() != null
                ? account.getAccountType().name() : null);
        response.setCurrentBalance(account.getCurrentBalance() != null
                ? account.getCurrentBalance().toString() : null);
        response.setAvailableBalance(account.getAvailableBalance() != null
                ? account.getAvailableBalance().toString() : null);
        response.setBalanceUpdatedAt(account.getBalanceUpdatedAt() != null
                ? account.getBalanceUpdatedAt().format(DATE_FORMAT) : null);
        response.setActive(account.isActive());
        response.setCreatedAt(account.getCreatedAt() != null
                ? account.getCreatedAt().format(DATE_FORMAT) : null);
        response.setUpdatedAt(account.getUpdatedAt() != null
                ? account.getUpdatedAt().format(DATE_FORMAT) : null);
        return response;
    }

    //CreateDTO => Entity
    //connection resolved in service layer before calling this mapper
    //userId comes from the authenticated request header, not the DTO body
    public BankAccount toEntity(BankAccountCreateDTO request, BankConnection connection, UUID userId) {
        BankAccount account = new BankAccount();
        account.setConnection(connection);
        account.setUserId(userId);
        account.setExternalAccountId(request.getExternalAccountId());
        account.setIban(request.getIban());
        account.setDisplayName(request.getDisplayName());
        account.setName(request.getName());
        account.setCurrency(request.getCurrency() != null ? request.getCurrency() : "PLN");
        account.setAccountType(request.getAccountType());
        account.setActive(true);
        return account;
    }

    //UpdateDTO => Entity - applies only non-null fields onto existing entity
    public BankAccount applyUpdate(BankAccount existing, BankAccountUpdateDTO request) {
        if (request.getDisplayName() != null) {
            existing.setDisplayName(request.getDisplayName());
        }
        return existing;
    }

    //Falls back to official bank name if user has not set a custom display name
    private String resolveDisplayName(BankAccount account) {
        if (account.getDisplayName() != null && !account.getDisplayName().isBlank()) {
            return account.getDisplayName();
        }
        return account.getName();
    }
}
