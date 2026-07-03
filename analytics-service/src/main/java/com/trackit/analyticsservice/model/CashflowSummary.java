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
@Table(name = "cashflow_summaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "period_year", "period_month"}))
public class CashflowSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    // Sum of all INBOUND bank transactions for the period
    @Column(name = "total_income", precision = 15, scale = 2)
    private BigDecimal totalIncome;

    // Sum of all OUTBOUND bank transactions for the period
    @Column(name = "total_expenses", precision = 15, scale = 2)
    private BigDecimal totalExpenses;

    // totalIncome - totalExpenses, negative means spending exceeded income
    @Column(name = "net_cashflow", precision = 15, scale = 2)
    private BigDecimal netCashflow;

    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Column(name = "currency", length = 3)
    private String currency;

    // Refreshed on every sync run for the same period
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}