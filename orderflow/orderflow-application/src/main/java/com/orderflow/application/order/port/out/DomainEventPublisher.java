package com.orderflow.application.order.port.out;

import com.orderflow.domain.shared.DomainEvent;

import java.util.List;

/**
 * Output port for emitting domain events to the outside world. Slice 1 ships a
 * logging adapter; slice 3 will replace it with a transactional outbox without
 * touching the application layer — the whole point of depending on this port.
 */
public interface DomainEventPublisher {

    void publish(List<DomainEvent> events);
}
