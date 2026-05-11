package com.trackit.bankaccountservice.repository;

import com.trackit.bankaccountservice.model.TransactionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRuleRepository extends JpaRepository<TransactionRule, UUID> {

    // Active rules only, ordered by priority descending - highest priority evaluated first
    List<TransactionRule> findByUserIdAndActiveTrueOrderByPriorityDesc(UUID userId);

    // Ownership check - always verify rule belongs to this user before any operation
    Optional<TransactionRule> findByIdAndUserId(UUID id, UUID userId);
}