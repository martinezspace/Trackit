package com.trackit.investmentservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "import_batches")
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    //Which account this import belongs to
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount account;

    //Original filename uploaded by user - useful for display and debug
    @NotBlank
    @Size(max = 255)
    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    //S3 key where file is stored, for now nullable until I configure AWS
    @Size(max = 512)
    @Column(name = "s3_key", length = 512)
    private String s3Key;

    //Which broker format was detected, decideds which parser runs
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "broker_format", nullable = false, length = 50)
    private BrokerFormat brokerFormat;

    //Lifecycle status of the import
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ImportStatus status = ImportStatus.PENDING;

    //Counts - nullable until processing starts
    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "imported_count")
    private Integer importedCount;

    @Column(name = "error_count")
    private Integer errorCount;

    //JSONB in PostgreSQL, TEXT in H2 for local dev
    //JSONB for quering, fileting and indexing individual fields inside
    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    //When the import finished - null until COMPLETED or FAILED
    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
