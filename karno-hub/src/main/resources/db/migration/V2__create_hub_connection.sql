CREATE TABLE IF NOT EXISTS hub_connection (
    id UUID PRIMARY KEY,
    from_hub_id UUID NOT NULL,
    to_hub_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_hub_connection_from_to UNIQUE (from_hub_id, to_hub_id)
);
