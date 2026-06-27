package com.orderflow.infrastructure.order.outbox;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** One stored domain event awaiting relay. */
@Entity
@Table(name = "outbox")
public class OutboxEntry {

    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OutboxEntry() {
    }

    public OutboxEntry(UUID id, UUID aggregateId, String type, String payload,
                       Instant occurredAt, Instant createdAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.published = false;
        this.createdAt = createdAt;
    }

    public void markPublished() {
        this.published = true;
    }

    public UUID getId() { return id; }
    public UUID getAggregateId() { return aggregateId; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
    public Instant getOccurredAt() { return occurredAt; }
    public boolean isPublished() { return published; }
}
