package com.trackit.bankaccountservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private BankAccount account;

    // Cross-service reference - denormalized to avoid joining through account on every query
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    // Tink transaction ID - used for idempotent import, prevents duplicates
    // Null for manually created transactions
    @Column(name = "external_id")
    private String externalId;

    // Always stored as a positive number - direction field determines inbound/outbound
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionDirection direction;

    // ISO 4217 - may differ from account currency for foreign transactions
    @Column(nullable = false, length = 3)
    private String currency;

    // Date the transaction was booked/settled by the bank
    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    // Date initiated - may differ from booking date for card payments
    @Column(name = "value_date")
    private LocalDate valueDate;

    // Raw description from the bank - often messy e.g. "CARD PAYMENT TO AMZN*AB1234"
    @Column(columnDefinition = "TEXT")
    private String description;

    // Cleaned merchant name - extracted automatically or set by user
    @Column(name = "merchant_name")
    private String merchantName;

    // Assigned spending category - null until categorized
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorization_status", nullable = false, length = 20)
    private CategorizationStatus categorizationStatus = CategorizationStatus.UNCATEGORIZED;

    // True for transactions created by the user manually, not imported from Tink
    // Manual transactions can be deleted; imported ones cannot
    @Column(name = "is_manual", nullable = false)
    private boolean manual = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}