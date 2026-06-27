package com.orderflow.infrastructure.messaging;

/**
 * Seam over the external message broker. The outbox relay talks to this
 * interface, so switching transport (log, Kafka, RabbitMQ) is a wiring choice,
 * not a code change in the relay or anything above it.
 */
public interface MessageBroker {

    void send(String type, String aggregateId, String payload);
}
