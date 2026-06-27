package com.orderflow.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Default broker: logs instead of publishing. Keeps local runs and tests free of
 * any broker infrastructure. Active unless the {@code kafka} profile is on.
 */
@Component
@Profile("!kafka")
public class LoggingMessageBroker implements MessageBroker {

    private static final Logger log = LoggerFactory.getLogger(LoggingMessageBroker.class);

    @Override
    public void send(String type, String aggregateId, String payload) {
        log.info("[log-broker] type={} aggregate={} payload={}", type, aggregateId, payload);
    }
}
