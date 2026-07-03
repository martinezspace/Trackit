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
@Table(name = "asset_allocation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "account_id"}))
public class AssetAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    // Cross-service reference — account lives in investment-service
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    // Denormalized for read performance — avoids calling investment-service on every analytics query
    @Column(name = "account_display_name", length = 100)
    private String accountDisplayName;

    @Column(name = "account_type", length = 20)
    private String accountType;

    // Currently mirrors account_type — refine to per-instrument breakdown once HoldingDTO exposes instrument_type
    @Column(name = "instrument_type", length = 20)
    private String instrumentType;

    // Sum of currentValue across all holdings in this account
    @Column(name = "current_value", precision = 15, scale = 2)
    private BigDecimal currentValue;

    // currentValue / total portfolio value * 100
    @Column(name = "weight_pct", precision = 8, scale = 4)
    private BigDecimal weightPct;

    @Column(name = "currency", length = 3)
    private String currency;

    // Represents current state, not history — fully replaced on every sync
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}