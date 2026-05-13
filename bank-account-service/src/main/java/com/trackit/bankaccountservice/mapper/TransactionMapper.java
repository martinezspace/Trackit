package com.trackit.bankaccountservice.mapper;

import com.trackit.bankaccountservice.dto.TransactionCreateDTO;
import com.trackit.bankaccountservice.dto.TransactionResponseDTO;
import com.trackit.bankaccountservice.dto.TransactionUpdateDTO;
import com.trackit.bankaccountservice.model.*;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class TransactionMapper {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_ONLY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    // Entity => ResponseDTO
    public TransactionResponseDTO toResponseDTO(Transaction transaction) {
        TransactionResponseDTO response = new TransactionResponseDTO();
        response.setId(transaction.getId().toString());
        response.setAccountId(transaction.getAccount().getId().toString());
        response.setUserId(transaction.getUserId().toString());
        response.setExternalId(transaction.getExternalId());
        response.setAmount(transaction.getAmount().toString());
        response.setDirection(transaction.getDirection().name());
        response.setCurrency(transaction.getCurrency());
        response.setBookingDate(transaction.getBookingDate().format(DATE_ONLY_FORMAT));
        response.setValueDate(transaction.getValueDate() != null
                ? transaction.getValueDate().format(DATE_ONLY_FORMAT) : null);
        response.setDescription(transaction.getDescription());
        response.setMerchantName(transaction.getMerchantName());
        response.setCategoryId(transaction.getCategory() != null
                ? transaction.getCategory().getId().toString() : null);
        response.setCategoryName(transaction.getCategory() != null
                ? transaction.getCategory().getName() : null);
        response.setCategorizationStatus(transaction.getCategorizationStatus().name());
        response.setManual(transaction.isManual());
        response.setNotes(transaction.getNotes());
        response.setCreatedAt(transaction.getCreatedAt() != null
                ? transaction.getCreatedAt().format(DATE_FORMAT) : null);
        response.setUpdatedAt(transaction.getUpdatedAt() != null
                ? transaction.getUpdatedAt().format(DATE_FORMAT) : null);
        return response;
    }

    // CreateDTO => Entity
    // account resolved in service layer before calling this mapper
    public Transaction toEntity(TransactionCreateDTO request, BankAccount account, UUID userId) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setUserId(userId);
        transaction.setExternalId(request.getExternalId());
        transaction.setAmount(request.getAmount());
        transaction.setDirection(request.getDirection());
        transaction.setCurrency(request.getCurrency());
        transaction.setBookingDate(request.getBookingDate());
        transaction.setValueDate(request.getValueDate());
        transaction.setDescription(request.getDescription());
        transaction.setMerchantName(request.getMerchantName());
        transaction.setManual(request.isManual());
        transaction.setNotes(request.getNotes());
        transaction.setCategorizationStatus(CategorizationStatus.UNCATEGORIZED);
        return transaction;
    }

    // UpdateDTO => Entity - applies only non-null fields onto existing entity
    public Transaction applyUpdate(Transaction existing, TransactionUpdateDTO request, Category category) {
        if (category != null) {
            existing.setCategory(category);
            // User explicitly chose a category - mark MANUAL so rule engine never overwrites it
            existing.setCategorizationStatus(CategorizationStatus.MANUAL);
        }
        if (request.getMerchantName() != null) existing.setMerchantName(request.getMerchantName());
        if (request.getNotes() != null) existing.setNotes(request.getNotes());
        return existing;
    }
}