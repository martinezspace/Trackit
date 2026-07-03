package com.trackit.analyticsservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "asset_allocation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "account_id"}))
@Getter
@Setter
public class AssetAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "account_display_name", length = 100)
    private String accountDisplayName;

    @Column(name = "account_type", length = 20)
    private String accountType;

    // Stores account_type for now — refine to per-instrument breakdown after HoldingDTO exposes instrument_type
    @Column(name = "instrument_type", length = 20)
    private String instrumentType;

    @Column(name = "current_value", precision = 15, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "weight_pct", precision = 8, scale = 4)
    private BigDecimal weightPct;

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