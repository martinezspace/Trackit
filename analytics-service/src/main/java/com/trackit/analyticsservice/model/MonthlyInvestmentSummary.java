package com.trackit.analyticsservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "monthly_investment_summaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "account_id", "period_year", "period_month"}))
@Getter
@Setter
public class MonthlyInvestmentSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "account_type", length = 20)
    private String accountType;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(name = "contributions_total", precision = 15, scale = 2)
    private BigDecimal contributionsTotal;

    @Column(name = "withdrawals_total", precision = 15, scale = 2)
    private BigDecimal withdrawalsTotal;

    @Column(name = "dividends_total", precision = 15, scale = 2)
    private BigDecimal dividendsTotal;

    @Column(name = "fees_total", precision = 15, scale = 2)
    private BigDecimal feesTotal;

    @Column(name = "taxes_total", precision = 15, scale = 2)
    private BigDecimal taxesTotal;

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