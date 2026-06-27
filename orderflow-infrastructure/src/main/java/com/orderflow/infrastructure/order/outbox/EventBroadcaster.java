package com.orderflow.infrastructure.order.outbox;

import com.orderflow.infrastructure.messaging.MessageBroker;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

/**
 * Resilience boundary in front of the message broker. Transient failures are
 * retried; if the broker stays unhealthy the circuit opens to stop hammering it.
 * A failure here propagates so the relay leaves the event unpublished and retries
 * it later (at-least-once delivery). The actual transport is chosen by profile
 * via {@link MessageBroker}.
 */
@Component
public class EventBroadcaster {

    private final MessageBroker broker;

    public EventBroadcaster(MessageBroker broker) {
        this.broker = broker;
    }

    @Retry(name = "broker")
    @CircuitBreaker(name = "broker")
    public void broadcast(String type, String aggregateId, String payload) {
        broker.send(type, aggregateId, payload);
    }
}
