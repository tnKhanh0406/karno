CREATE TABLE IF NOT EXISTS shipper_shift (
    id UUID PRIMARY KEY,
    shipper_user_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    hub_id UUID NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (starts_at < ends_at)
);