package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.dto.TransactionRuleCreateDTO;
import com.trackit.bankaccountservice.dto.TransactionRuleResponseDTO;
import com.trackit.bankaccountservice.dto.TransactionRuleUpdateDTO;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.mapper.TransactionRuleMapper;
import com.trackit.bankaccountservice.model.Category;
import com.trackit.bankaccountservice.model.TransactionRule;
import com.trackit.bankaccountservice.repository.CategoryRepository;
import com.trackit.bankaccountservice.repository.TransactionRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TransactionRuleService {

    private final TransactionRuleRepository ruleRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRuleMapper mapper;

    // Queries

    public List<TransactionRuleResponseDTO> getAllRulesForUser(UUID userId) {
        return ruleRepository.findByUserIdAndActiveTrueOrderByPriorityDesc(userId).stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    public TransactionRuleResponseDTO getRuleById(UUID id, UUID userId) {
        TransactionRule rule = ruleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));
        return mapper.toResponseDTO(rule);
    }

    // Commands

    public TransactionRuleResponseDTO createRule(UUID userId, TransactionRuleCreateDTO request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + request.getCategoryId()));
        TransactionRule rule = mapper.toEntity(request, category, userId);
        return mapper.toResponseDTO(ruleRepository.save(rule));
    }

    public TransactionRuleResponseDTO updateRule(UUID id, UUID userId, TransactionRuleUpdateDTO request) {
        TransactionRule existing = ruleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));

        // Resolve new category only if caller is changing it
        Category category = request.getCategoryId() != null
                ? categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + request.getCategoryId()))
                : null;

        TransactionRule updated = mapper.applyUpdate(existing, request, category);
        return mapper.toResponseDTO(ruleRepository.save(updated));
    }

    public void deleteRule(UUID id, UUID userId) {
        TransactionRule existing = ruleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));
        // Soft delete - keeps history and allows recovery if deleted by mistake
        existing.setActive(false);
        ruleRepository.save(existing);
    }
}