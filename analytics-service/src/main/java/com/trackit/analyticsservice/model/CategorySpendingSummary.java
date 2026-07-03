package com.trackit.analyticsservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "category_spending_summaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category_id", "period_year", "period_month"}))
public class CategorySpendingSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    // Cross-service reference — category lives in bank-account-service, denormalized here to avoid runtime joins
    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    // Denormalized for read performance — avoids calling bank-account-service on every analytics query
    @Column(name = "category_name", length = 100)
    private String categoryName;

    @Column(name = "category_color", length = 7)
    private String categoryColor;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    // Sum of OUTBOUND transactions for this category in the period
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Column(name = "currency", length = 3)
    private String currency;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}