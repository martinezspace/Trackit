package com.trackit.bankaccountservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    //The Tink connection this account was imported through
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id", nullable = false, updatable = false)
    private BankConnection connection;

    //Cross-service reference
    @NotNull
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    //Tink account ID - used to fetch transactions from their API
    @NotNull
    @Column(name = "external_account_id", nullable = false, updatable = false)
    private String externalAccountId;

    //IBAN - may be null for some account types
    @Size(max = 34)
    @Column(name = "iban", length = 34)
    private String iban;

    //User defined label shown in the UI - fals back to name if null
    @Size(max = 100)
    @Column(name = "display_name", length = 100)
    private String displayName;

    //Official account name from the bank e.g. "Current Account"
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    //e.g PLN, EUR, USD, GBP
    @Size(min = 3, max = 3)
    @NotNull
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20)
    private AccountType accountType;

    //Last known balance fetched from Tink - not real-time
    @Column(name = "current_balance", precision = 15, scale = 2)
    private BigDecimal currentBalance;

    //Available balance excludes pending transactions - may differ from current
    @Column(name = "available_balance", precision = 15, scale = 2)
    private BigDecimal availableBalance;

    //When balances were last fetched - used to show staleness wrning in UI
    @Column(name = "balance_updated_at")
    private LocalDateTime balanceUpdatedAt;

    //Soft delete flag - deactivated instead of deleted to preserve transaction history
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
