CREATE SEQUENCE outbox_event_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE outbox_event (
    id BIGINT PRIMARY KEY,
    exchange VARCHAR(255) NOT NULL,
    routing_key VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE INDEX outbox_event_unpublished_idx
ON outbox_event(id)
WHERE (published_at IS NULL);
