CREATE TABLE t_outbox_event (
    id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    dedupe_key VARCHAR(255) NULL,
    available_at DATETIME(6) NOT NULL,
    retry_count INT NOT NULL,
    last_error TEXT NULL,
    processed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_outbox_event PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_event_status_available_id
    ON t_outbox_event (status, available_at, id);

CREATE INDEX idx_outbox_event_dedupe_key
    ON t_outbox_event (dedupe_key);

CREATE INDEX idx_outbox_event_aggregate
    ON t_outbox_event (aggregate_type, aggregate_id);
