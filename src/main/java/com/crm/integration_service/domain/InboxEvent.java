package com.crm.integration_service.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "inbox_events",
    uniqueConstraints = @UniqueConstraint(name = "uq_inbox_event_id", columnNames = "event_id")
)
public class InboxEvent {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false, length = 128)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private InboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected InboxEvent() {}

    public static InboxEvent newPending(String eventId, String eventType, String payload, String correlationId) {
        OffsetDateTime now = OffsetDateTime.now();
        InboxEvent e = new InboxEvent();
        e.id = UUID.randomUUID();
        e.eventId = eventId;
        e.eventType = eventType;
        e.payload = payload;
        e.status = InboxStatus.PENDING;
        e.attemptCount = 0;
        e.nextAttemptAt = now;
        e.correlationId = correlationId;
        e.createdAt = now;
        e.updatedAt = now;
        return e;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getEventId() { return eventId; }
   // public int getAttemptCount1() { return attemptCount; }
    public void markProcessing() {
        this.status = InboxStatus.PROCESSING;
    }

    public void markProcessed() {
        this.status = InboxStatus.PROCESSED;
    }

    public void markFailed(String err, int attempt, OffsetDateTime nextAttemptAt) {
        this.status = InboxStatus.FAILED;
        this.attemptCount = attempt;
        this.nextAttemptAt = nextAttemptAt;
    }

    public void markPendingRetry(int attempt, OffsetDateTime nextAttemptAt) {
        this.status = InboxStatus.PENDING;
        this.attemptCount = attempt;
        this.nextAttemptAt = nextAttemptAt;
    }

    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public int getAttemptCount() { return attemptCount; }
     public String getCorrelationId(){
        return correlationId;
    }
    public void resetForReplay() {
        this.status = InboxStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = OffsetDateTime.now();
    }

}

