package com.trackit.analyticsservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "category_spending_summaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category_id", "period_year", "period_month"}))
@Getter
@Setter
public class CategorySpendingSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "category_name", length = 100)
    private String categoryName;

    @Column(name = "category_color", length = 7)
    private String categoryColor;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}