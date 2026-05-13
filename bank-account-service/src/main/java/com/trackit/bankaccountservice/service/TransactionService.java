package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.dto.PagedResponseDTO;
import com.trackit.bankaccountservice.dto.TransactionCreateDTO;
import com.trackit.bankaccountservice.dto.TransactionResponseDTO;
import com.trackit.bankaccountservice.dto.TransactionUpdateDTO;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.mapper.TransactionMapper;
import com.trackit.bankaccountservice.model.*;
import com.trackit.bankaccountservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRuleRepository ruleRepository;
    private final TransactionMapper mapper;

    // Queries

    public PagedResponseDTO<TransactionResponseDTO> getTransactions(
            UUID userId, UUID accountId, UUID categoryId,
            TransactionDirection direction, LocalDate from, LocalDate to,
            BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable) {

        return PagedResponseDTO.from(
                transactionRepository.findFiltered(
                                userId, accountId, categoryId, direction,
                                from, to, minAmount, maxAmount, pageable)
                        .map(mapper::toResponseDTO));
    }

    public TransactionResponseDTO getTransactionById(UUID id, UUID userId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        return mapper.toResponseDTO(transaction);
    }

    // Commands

    @Transactional
    public TransactionResponseDTO createTransaction(UUID userId, TransactionCreateDTO request) {
        BankAccount account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountId()));

        // Idempotency - if this external transaction was already imported return existing
        if (request.getExternalId() != null) {
            return transactionRepository.findByAccountIdAndExternalId(
                            account.getId(), request.getExternalId())
                    .map(mapper::toResponseDTO)
                    .orElseGet(() -> saveNewTransaction(request, account, userId));
        }

        return saveNewTransaction(request, account, userId);
    }

    public TransactionResponseDTO updateTransaction(UUID id, UUID userId, TransactionUpdateDTO request) {
        Transaction existing = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));

        // Resolve category only if caller is changing it
        Category category = request.getCategoryId() != null
                ? categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + request.getCategoryId()))
                : null;

        Transaction updated = mapper.applyUpdate(existing, request, category);
        return mapper.toResponseDTO(transactionRepository.save(updated));
    }

    public void deleteTransaction(UUID id, UUID userId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));

        // Imported transactions are the source of truth - only manual ones can be removed
        if (!transaction.isManual()) {
            throw new IllegalArgumentException("Only manually created transactions can be deleted");
        }

        transactionRepository.delete(transaction);
    }

    // Re-runs all active rules against uncategorized transactions for a user
    // Called after a user creates or modifies a rule so it applies retroactively
    @Transactional
    public int reapplyRules(UUID userId) {
        List<TransactionRule> rules = ruleRepository.findByUserIdAndActiveTrueOrderByPriorityDesc(userId);
        if (rules.isEmpty()) return 0;

        List<Transaction> uncategorized = transactionRepository
                .findByUserIdAndCategorizationStatus(
                        userId, CategorizationStatus.UNCATEGORIZED, Pageable.unpaged())
                .getContent();

        int count = 0;
        for (Transaction transaction : uncategorized) {
            if (applyRules(transaction, rules)) {
                transactionRepository.save(transaction);
                count++;
            }
        }
        return count;
    }

    // Helpers

    private TransactionResponseDTO saveNewTransaction(TransactionCreateDTO request,
                                                      BankAccount account, UUID userId) {
        Transaction transaction = mapper.toEntity(request, account, userId);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found: " + request.getCategoryId()));
            transaction.setCategory(category);
            transaction.setCategorizationStatus(CategorizationStatus.MANUAL);
        } else {
            List<TransactionRule> rules = ruleRepository
                    .findByUserIdAndActiveTrueOrderByPriorityDesc(userId);
            applyRules(transaction, rules);
        }

        return mapper.toResponseDTO(transactionRepository.save(transaction));
    }

    // Evaluates rules in priority order - first match wins
    // Returns true if a category was assigned
    private boolean applyRules(Transaction transaction, List<TransactionRule> rules) {
        for (TransactionRule rule : rules) {
            if (matches(rule, transaction)) {
                transaction.setCategory(rule.getCategory());
                transaction.setCategorizationStatus(CategorizationStatus.AUTO);
                return true;
            }
        }
        return false;
    }

    private boolean matches(TransactionRule rule, Transaction transaction) {
        if (rule.getAmountMin() != null
                && transaction.getAmount().compareTo(rule.getAmountMin()) < 0) return false;
        if (rule.getAmountMax() != null
                && transaction.getAmount().compareTo(rule.getAmountMax()) > 0) return false;

        String pattern = rule.getMatchPattern().toLowerCase();

        return switch (rule.getMatchField()) {
            case DESCRIPTION -> transaction.getDescription() != null
                    && transaction.getDescription().toLowerCase().contains(pattern);
            case MERCHANT -> transaction.getMerchantName() != null
                    && transaction.getMerchantName().toLowerCase().contains(pattern);
            case BOTH -> (transaction.getDescription() != null
                    && transaction.getDescription().toLowerCase().contains(pattern))
                    || (transaction.getMerchantName() != null
                    && transaction.getMerchantName().toLowerCase().contains(pattern));
        };
    }
}