package com.orderflow.infrastructure.order.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.application.order.port.out.DomainEventPublisher;
import com.orderflow.domain.order.event.OrderEvent;
import com.orderflow.domain.shared.DomainEvent;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implements the {@link DomainEventPublisher} port by writing events into the
 * outbox table. Because this runs inside the use case's transaction, the event
 * rows commit atomically with the order change — the essence of the pattern.
 * A separate relay ({@link OutboxRelay}) publishes them afterwards.
 */
@Component
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxDomainEventPublisher(OutboxRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void publish(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            repository.save(new OutboxEntry(
                UUID.randomUUID(),
                aggregateIdOf(event),
                event.getClass().getSimpleName(),
                serialize(event),
                event.occurredAt(),
                Instant.now(clock)));
        }
    }

    private UUID aggregateIdOf(DomainEvent event) {
        // Every order event exposes its aggregate id; default to a nil UUID otherwise.
        return event instanceof OrderEvent orderEvent ? orderEvent.orderId().value() : new UUID(0, 0);
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize domain event " + event, e);
        }
    }
}
