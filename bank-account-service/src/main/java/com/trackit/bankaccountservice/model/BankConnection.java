package com.trackit.bankaccountservice.model;

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
@Table(name = "bank_connections")
public class BankConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    //Cross-service reference - plain UUID
    @NotNull
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @NotNull
    @Size(max = 100)
    @Column(name = "institution_id", nullable = false, length = 100)
    private String institutionId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "institution_name", nullable = false)
    private String institutionName;

    // Tink user ID for this end-user — set once on first connection, reused for subsequent ones
    @Size(max = 255)
    @Column(name = "tink_user_id", length = 255)
    private String tinkUserId;

    // Tink credentials ID — identifies the bank connection, set after user completes auth
    @Size(max = 255)
    @Column(name = "credentials_id", length = 255)
    private String credentialsId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConnectionStatus status;

    // Bank consent typically expires after 90 days — used to warn users proactively
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    //Updated by sync logic each time a sync completes successfully
    //Used for incremental sync - fetch only transaction since this timestamp
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
