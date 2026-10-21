ALTER TABLE shipments
    ADD COLUMN pickup_name VARCHAR(255),
    ADD COLUMN tracking_code VARCHAR(100);

CREATE UNIQUE INDEX uk_shipments_tracking_code
    ON shipments (tracking_code)
    WHERE tracking_code IS NOT NULL;

CREATE TABLE processed_events (
    event_id UUID NOT NULL,
    consumer_name VARCHAR(150) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id, consumer_name)
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shipping_outbox_pending
    ON outbox_events (status, created_at);
