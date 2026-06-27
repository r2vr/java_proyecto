package com.orderflow.domain.shared;

import java.time.Instant;

/**
 * Marker for facts that have happened in the domain and that other parts of the
 * system may react to (e.g. an order was confirmed).
 * <p>
 * Events are immutable and carry the instant at which they occurred. They are
 * raised inside aggregates and pulled out by the application layer to be
 * published (transactional outbox, message broker...).
 */
public interface DomainEvent {

    /** When the fact occurred, in UTC. */
    Instant occurredAt();
}
