package com.trackit.bankaccountservice.repository;

import com.trackit.bankaccountservice.model.CategorizationStatus;
import com.trackit.bankaccountservice.model.Transaction;
import com.trackit.bankaccountservice.model.TransactionDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // Ownership check - always verify transaction belongs to this user before any operation
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    // Idempotency check during import - prevents duplicate transactions from Tink
    Optional<Transaction> findByAccountIdAndExternalId(UUID accountId, String externalId);

    // Filtered list - all params optional, null means no filter applied
    // Paginated because users can have thousands of transactions
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.userId = :userId
        AND (:accountId IS NULL OR t.account.id = :accountId)
        AND (:categoryId IS NULL OR t.category.id = :categoryId)
        AND (:direction IS NULL OR t.direction = :direction)
        AND (:from IS NULL OR t.bookingDate >= :from)
        AND (:to IS NULL OR t.bookingDate <= :to)
        AND (:minAmount IS NULL OR t.amount >= :minAmount)
        AND (:maxAmount IS NULL OR t.amount <= :maxAmount)
        ORDER BY t.bookingDate DESC
        """)
    Page<Transaction> findFiltered(
            @Param("userId") UUID userId,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("direction") TransactionDirection direction,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable
    );

    // Used by rule re-application - fetch all uncategorized transactions for a user
    Page<Transaction> findByUserIdAndCategorizationStatus(
            UUID userId, CategorizationStatus status, Pageable pageable);
}