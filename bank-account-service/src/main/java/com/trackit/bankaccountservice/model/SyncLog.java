package com.trackit.bankaccountservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "sync_logs")
public class SyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id", nullable = false, updatable = false)
    private BankConnection connection;

    // Null when sync covers all accounts under the connection
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private BankAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncTrigger trigger;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncStatus status;

    // Total transactions returned by GoCardless for this sync window
    @Column(name = "transactions_fetched", nullable = false)
    private int transactionsFetched = 0;

    // Transactions that did not already exist in our database
    @Column(name = "transactions_new", nullable = false)
    private int transactionsNew = 0;

    // Transactions where rule engine found a category match
    @Column(name = "transactions_suggested", nullable = false)
    private int transactionsSuggested = 0;

    // Populated when status is FAILED - raw error message for debugging
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    // Null while sync is still RUNNING
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}