package com.trackit.bankaccountservice.mapper;

import com.trackit.bankaccountservice.dto.TransactionRuleCreateDTO;
import com.trackit.bankaccountservice.dto.TransactionRuleResponseDTO;
import com.trackit.bankaccountservice.dto.TransactionRuleUpdateDTO;
import com.trackit.bankaccountservice.model.Category;
import com.trackit.bankaccountservice.model.TransactionRule;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class TransactionRuleMapper {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Entity => ResponseDTO
    public TransactionRuleResponseDTO toResponseDTO(TransactionRule rule) {
        TransactionRuleResponseDTO response = new TransactionRuleResponseDTO();
        response.setId(rule.getId().toString());
        response.setUserId(rule.getUserId().toString());
        response.setName(rule.getName());
        response.setMatchField(rule.getMatchField().name());
        response.setMatchPattern(rule.getMatchPattern());
        response.setAmountMin(rule.getAmountMin() != null ? rule.getAmountMin().toString() : null);
        response.setAmountMax(rule.getAmountMax() != null ? rule.getAmountMax().toString() : null);
        response.setCategoryId(rule.getCategory().getId().toString());
        response.setCategoryName(rule.getCategory().getName());
        response.setPriority(rule.getPriority());
        response.setActive(rule.isActive());
        response.setCreatedAt(rule.getCreatedAt() != null
                ? rule.getCreatedAt().format(DATE_FORMAT) : null);
        response.setUpdatedAt(rule.getUpdatedAt() != null
                ? rule.getUpdatedAt().format(DATE_FORMAT) : null);
        return response;
    }

    // CreateDTO => Entity
    // category resolved in service layer before calling this mapper
    // userId comes from the authenticated request header, not the DTO body
    public TransactionRule toEntity(TransactionRuleCreateDTO request, Category category, UUID userId) {
        TransactionRule rule = new TransactionRule();
        rule.setUserId(userId);
        rule.setName(request.getName());
        rule.setMatchField(request.getMatchField());
        rule.setMatchPattern(request.getMatchPattern());
        rule.setAmountMin(request.getAmountMin());
        rule.setAmountMax(request.getAmountMax());
        rule.setCategory(category);
        rule.setPriority(request.getPriority());
        rule.setActive(true);
        return rule;
    }

    // UpdateDTO => Entity - applies only non-null fields onto existing entity
    // category resolved in service layer before calling this mapper
    public TransactionRule applyUpdate(TransactionRule existing, TransactionRuleUpdateDTO request, Category category) {
        if (request.getName() != null) existing.setName(request.getName());
        if (request.getMatchField() != null) existing.setMatchField(request.getMatchField());
        if (request.getMatchPattern() != null) existing.setMatchPattern(request.getMatchPattern());
        if (request.getAmountMin() != null) existing.setAmountMin(request.getAmountMin());
        if (request.getAmountMax() != null) existing.setAmountMax(request.getAmountMax());
        if (category != null) existing.setCategory(category);
        if (request.getPriority() != null) existing.setPriority(request.getPriority());
        return existing;
    }
}