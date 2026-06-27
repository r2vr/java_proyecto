package com.orderflow.infrastructure.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Real broker: publishes events to Kafka keyed by aggregate id (so a given
 * order's events stay ordered within a partition). Active under the {@code kafka}
 * profile. A send failure propagates to the resilience layer and, ultimately,
 * leaves the outbox entry for retry.
 */
@Component
@Profile("kafka")
public class KafkaMessageBroker implements MessageBroker {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public KafkaMessageBroker(KafkaTemplate<String, String> kafkaTemplate,
                              @Value("${orderflow.kafka.topic:orders.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void send(String type, String aggregateId, String payload) {
        // .join() makes the publish synchronous so failures surface to the relay.
        kafkaTemplate.send(topic, aggregateId, payload).join();
    }
}
