package com.trackit.investmentservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "investment_transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_account_external_id",
                        columnNames = {"account_id", "external_id"}
                )
        }
)
public class InvestmentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount account;

    // Nullable - will be populated for CSV imports, null until import_batches is built
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private ImportBatch batch;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    //Brokers own trans ID - used for deduplication during CSV import
    //Nullable - manual entries won't have external id
    @Size(max = 255)
    @Column(name = "external_id", length = 255)
    private String externalId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    //6 decimal places
    @NotNull
    @Column(name = "quantity", nullable = false, precision = 15, scale = 6)
    private BigDecimal quantity;

    //Price per unit at time of transaction
    @NotNull
    @Column(name = "price", nullable = false, precision = 15, scale = 4)
    private BigDecimal price;

    //Total transaction value
    @NotNull
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Size(min = 3, max = 3)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @NotNull
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    //Soft cancel - never delete transactions
    @Column(name = "is_cancelled", nullable = false)
    private boolean cancelled = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
