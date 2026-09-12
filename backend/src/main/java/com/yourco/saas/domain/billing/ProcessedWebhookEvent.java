package com.yourco.saas.domain.billing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "processed_webhook_events")
public class ProcessedWebhookEvent {

    @Id
    @Column(name = "stripe_event_id")
    private String stripeEventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected ProcessedWebhookEvent() {}

    public ProcessedWebhookEvent(String stripeEventId, String eventType) {
        this.stripeEventId = stripeEventId;
        this.eventType = eventType;
    }

    public String getStripeEventId() { return stripeEventId; }
    public String getEventType() { return eventType; }
    public Instant getProcessedAt() { return processedAt; }
}