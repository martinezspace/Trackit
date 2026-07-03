package com.trackit.analyticsservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "net_worth_snapshots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "snapshot_date"}))
public class NetWorthSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    // One snapshot per user per day — sync overwrites if already exists for today
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    // Sum of currentBalance across all active bank accounts
    @Column(name = "bank_balance_total", precision = 15, scale = 2)
    private BigDecimal bankBalanceTotal;

    // Sum of totalValue from latest portfolio snapshot per investment account
    @Column(name = "investment_value_total", precision = 15, scale = 2)
    private BigDecimal investmentValueTotal;

    @Column(name = "net_worth", precision = 15, scale = 2)
    private BigDecimal netWorth;

    @Column(name = "currency", length = 3)
    private String currency;

    // Snapshots are immutable once written — no updatedAt needed
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}