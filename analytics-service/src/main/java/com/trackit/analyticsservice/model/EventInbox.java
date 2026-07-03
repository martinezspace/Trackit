package com.trackit.analyticsservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

// Transactional inbox pattern — events land here first, then a processor writes to summary tables
// Unused in polling mode; will be the entry point when Kafka is introduced
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "event_inbox")
public class EventInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // Idempotency key — prevents processing the same event twice on consumer restart
    @Column(name = "event_id", length = 255, nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    @Column(name = "source_service", length = 50, nullable = false)
    private String sourceService;

    // Raw event payload stored as JSON — deserialized by the processor based on event_type
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    // PENDING → PROCESSED or FAILED
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}