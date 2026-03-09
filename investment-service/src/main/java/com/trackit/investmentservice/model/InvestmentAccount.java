package com.trackit.investmentservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "investment_accounts")
public class InvestmentAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    private UUID id;

    //Cross-service reference - plain UUID
    @NotNull
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @NotBlank
    @Size(max = 100)
    @Column(name = "broker_name", nullable = false, length = 100)
    private String brokerName;

    //Nullable - user-defined label, most users won't set this initially
    @Size(max = 100)
    @Column(name = "display_name", length = 100)
    private String displayName;

    //Nullable - not all brokers provide one, user may not have it to hand
    @Size(max = 100)
    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @NotNull
    @Size(min = 3, max = 3)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "PLN";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    //Nullable - free text field, user fills in optionally
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
