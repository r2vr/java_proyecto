-- Transactional outbox: domain events are written here in the SAME transaction
-- as the business change, then relayed asynchronously. Guarantees an event is
-- stored if and only if its order change committed (no lost or phantom events).
CREATE TABLE outbox (
    id           UUID         PRIMARY KEY,
    aggregate_id UUID         NOT NULL,
    type         VARCHAR(100) NOT NULL,
    payload      TEXT         NOT NULL,   -- JSON; could be JSONB for indexing/querying
    occurred_at  TIMESTAMPTZ  NOT NULL,
    published    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL
);

-- Partial index: the relay only ever scans the unpublished tail.
CREATE INDEX idx_outbox_unpublished ON outbox (created_at) WHERE published = FALSE;
