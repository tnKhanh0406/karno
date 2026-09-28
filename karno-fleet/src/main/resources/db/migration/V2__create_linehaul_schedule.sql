CREATE TABLE IF NOT EXISTS linehaul_schedule (
    id UUID PRIMARY KEY,
    schedule_code VARCHAR(50) NOT NULL UNIQUE,
    from_hub_id UUID NOT NULL,
    to_hub_id UUID NOT NULL,
    departure_time TIME NOT NULL,
    frequency VARCHAR(50) NOT NULL DEFAULT 'DAILY',
    planned_duration_minutes INT NOT NULL,
    capacity_weight DECIMAL(12,2) NOT NULL DEFAULT 0,
    capacity_volume DECIMAL(12,2) NOT NULL DEFAULT 0,
    vehicle_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
