package com.crm.integration_service.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dlq_events")
public class DlqEvent {

    @Id
    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "reason", nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "failed_at", nullable = false)
    private OffsetDateTime failedAt;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    protected DlqEvent() {}

    public static DlqEvent of(String eventId, String eventType, String payload, String reason, String correlationId) {
        DlqEvent d = new DlqEvent();
        d.eventId = eventId;
        d.eventType = eventType;
        d.payload = payload;
        d.reason = reason;
        d.correlationId = correlationId;
        d.failedAt = OffsetDateTime.now();
        return d;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public String getReason() { return reason; }
    public String getCorrelationId() { return correlationId; }

   
}