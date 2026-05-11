package com.trackit.bankaccountservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "transaction_rules")
public class TransactionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    // Human-readable rule name e.g. "Lidl purchases"
    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_field", nullable = false, length = 20)
    private RuleMatchField matchField;

    // Case-insensitive substring to look for e.g. "lidl", "netflix"
    @Column(name = "match_pattern", nullable = false)
    private String matchPattern;

    // Optional amount range - null means no limit on that side
    @Column(name = "amount_min", precision = 15, scale = 2)
    private BigDecimal amountMin;

    @Column(name = "amount_max", precision = 15, scale = 2)
    private BigDecimal amountMax;

    // Category to assign when this rule matches
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Higher priority rules are evaluated first - first match wins
    @Column(nullable = false)
    private int priority = 0;

    // Soft delete - inactive rules skipped by engine but kept for history
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}