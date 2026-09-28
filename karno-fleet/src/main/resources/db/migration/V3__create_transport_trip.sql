CREATE TABLE IF NOT EXISTS transport_trip (
    id UUID PRIMARY KEY,
    trip_code VARCHAR(100) NOT NULL UNIQUE,
    schedule_id UUID NOT NULL,
    from_hub_id UUID NOT NULL,
    to_hub_id UUID NOT NULL,
    vehicle_id UUID,
    scheduled_departure_at TIMESTAMP NOT NULL,
    planned_arrival_at TIMESTAMP NOT NULL,
    estimated_arrival_at TIMESTAMP NOT NULL,
    actual_departure_at TIMESTAMP,
    actual_arrival_at TIMESTAMP,
    capacity_weight DECIMAL(12,2) NOT NULL DEFAULT 0,
    reserved_weight DECIMAL(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_schedule_departure UNIQUE (schedule_id, scheduled_departure_at)
);
