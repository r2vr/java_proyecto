package com.orderflow.infrastructure.order.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface OutboxRepository extends JpaRepository<OutboxEntry, UUID> {

    /** The oldest unpublished entries — the relay's work queue. */
    List<OutboxEntry> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
