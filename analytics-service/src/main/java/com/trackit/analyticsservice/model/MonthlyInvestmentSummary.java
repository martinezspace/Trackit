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
@Table(name = "monthly_investment_summaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "account_id", "period_year", "period_month"}))
public class MonthlyInvestmentSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    // Cross-service reference — account lives in investment-service
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "account_type", length = 20)
    private String accountType;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    // BUY transactions total for the period
    @Column(name = "contributions_total", precision = 15, scale = 2)
    private BigDecimal contributionsTotal;

    // SELL transactions total for the period
    @Column(name = "withdrawals_total", precision = 15, scale = 2)
    private BigDecimal withdrawalsTotal;

    // DIVIDEND transactions total for the period
    @Column(name = "dividends_total", precision = 15, scale = 2)
    private BigDecimal dividendsTotal;

    // Reserved for future TransactionType additions — zero until then
    @Column(name = "fees_total", precision = 15, scale = 2)
    private BigDecimal feesTotal;

    @Column(name = "taxes_total", precision = 15, scale = 2)
    private BigDecimal taxesTotal;

    @Column(name = "currency", length = 3)
    private String currency;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}