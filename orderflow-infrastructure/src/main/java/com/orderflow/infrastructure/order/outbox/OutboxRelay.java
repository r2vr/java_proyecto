package com.orderflow.infrastructure.order.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Polls the outbox and relays pending events, then marks them published — all in
 * one transaction so a crash mid-relay simply retries on the next tick
 * (at-least-once delivery).
 * <p>
 * Here "relay" logs the event; swapping in Kafka/RabbitMQ is a change to this one
 * class only — the application and domain layers never learn about the broker.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repository;
    private final EventBroadcaster broadcaster;

    public OutboxRelay(OutboxRepository repository, EventBroadcaster broadcaster) {
        this.repository = repository;
        this.broadcaster = broadcaster;
    }

    @Scheduled(fixedDelayString = "${orderflow.outbox.poll-interval-ms:2000}")
    @Transactional
    public void relayPending() {
        List<OutboxEntry> pending = repository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            return;
        }
        int relayed = 0;
        for (OutboxEntry entry : pending) {
            try {
                broadcaster.broadcast(entry.getType(), entry.getAggregateId().toString(), entry.getPayload());
                entry.markPublished();
                relayed++;
            } catch (Exception ex) {
                // Leave the entry unpublished so it is retried on the next tick.
                log.warn("Failed to relay outbox entry {}, will retry: {}", entry.getId(), ex.getMessage());
            }
        }
        log.info("Relayed {}/{} outbox event(s)", relayed, pending.size());
    }
}
