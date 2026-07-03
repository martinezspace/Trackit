package com.trackit.analyticsservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cashflow_summaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "period_year", "period_month"}))
@Getter
@Setter
public class CashflowSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(name = "total_income", precision = 15, scale = 2)
    private BigDecimal totalIncome;

    @Column(name = "total_expenses", precision = 15, scale = 2)
    private BigDecimal totalExpenses;

    @Column(name = "net_cashflow", precision = 15, scale = 2)
    private BigDecimal netCashflow;

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