package com.trackit.bankaccountservice.repository;

import com.trackit.bankaccountservice.model.TinkToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TinkTokenRepository extends JpaRepository<TinkToken, UUID> {

    // Single-row table — always retrieve the one stored token
    Optional<TinkToken> findFirstBy();
}