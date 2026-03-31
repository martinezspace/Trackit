package com.trackit.investmentservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "portfolio_snapshots",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_account_snapshot_date",
                        columnNames = {"account_id", "snapshot_date"}
                )
        }
)
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount account;

    //One snapshot per account per day - enforced by unique constraint
    @NotNull
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    //Sum of all holding current_value for this account
    @NotNull
    @Column(name = "total_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalValue;

    //Sum of all holding total_invested for this account
    @NotNull
    @Column(name = "total_invested", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInvested;

    //total_value - total_invested
    @NotNull
    @Column(name = "total_gain_loss", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalGainLoss;

    //total_gain_loss / total_invested x 100
    @NotNull
    @Column(name = "gain_loss_pct", nullable = false, precision = 8, scale = 4)
    private BigDecimal gainLossPct;

    @NotNull
    @Size(min = 3, max = 3)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
