package com.trackit.investmentservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.UUID;

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

    //Constructors

    protected InvestmentAccount() {}

    public static InvestmentAccount create(
            UUID userId,
            AccountType accountType,
            String brokerName,
            String displayName,
            String accountNumber,
            String currency,
            String notes
    ) {
        InvestmentAccount account = new InvestmentAccount();
        account.userId = userId;
        account.accountType = accountType;
        account.brokerName = brokerName;
        account.displayName = displayName;
        account.accountNumber = accountNumber;
        account.currency = currency != null ? currency : "PLN";
        account.notes = notes;
        account.active = true;
        return account;
    }

    //Business methods

    public String resolvedDisplayName() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return brokerName + " " + accountType.name();
    }

    public void deactivate() {
        this.active = false;
    }

    //Getter
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public AccountType getAccountType() { return accountType; }
    public String getBrokerName() { return brokerName; }
    public String getDisplayName() { return displayName; }
    public String getAccountNumber() { return accountNumber; }
    public String getCurrency() { return currency; }
    public boolean isActive() { return active; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    //Setters
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
