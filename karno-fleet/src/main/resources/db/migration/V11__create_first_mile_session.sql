CREATE TABLE service_zone (
    id UUID PRIMARY KEY,
    hub_id UUID NOT NULL,
    zone_code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    hub_latitude DECIMAL(9,6) NOT NULL,
    hub_longitude DECIMAL(10,6) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    UNIQUE (hub_id, zone_code)
);

CREATE TABLE shipper_zone_assignment (
    shipper_id UUID NOT NULL,
    zone_id UUID NOT NULL REFERENCES service_zone(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (shipper_id, zone_id)
);
CREATE UNIQUE INDEX uk_shipper_active_zone ON shipper_zone_assignment (shipper_id) WHERE active;

ALTER TABLE vehicle
    ADD COLUMN ownership_type VARCHAR(20) NOT NULL DEFAULT 'HUB',
    ADD COLUMN owner_shipper_id UUID,
    ADD COLUMN hub_id UUID;

ALTER TABLE pickup_request
    ADD COLUMN pickup_zone_id UUID,
    ADD COLUMN pickup_session_id UUID,
    ADD COLUMN assigned_shipper_id UUID;

CREATE TABLE pickup_session (
    id UUID PRIMARY KEY,
    shipper_id UUID NOT NULL,
    hub_id UUID NOT NULL,
    zone_id UUID NOT NULL REFERENCES service_zone(id),
    vehicle_id UUID NOT NULL REFERENCES vehicle(id),
    start_latitude DECIMAL(9,6) NOT NULL,
    start_longitude DECIMAL(10,6) NOT NULL,
    planned_start_at TIMESTAMP NOT NULL,
    desired_return_at TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    route_plan JSONB,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CHECK (planned_start_at < desired_return_at)
);
CREATE UNIQUE INDEX uk_active_pickup_session_shipper ON pickup_session (shipper_id) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uk_active_pickup_session_vehicle ON pickup_session (vehicle_id) WHERE status = 'ACTIVE';

UPDATE vehicle SET hub_id = 'c1000000-0000-0000-0000-000000000001' WHERE hub_id IS NULL;

INSERT INTO service_zone (id, hub_id, zone_code, name, hub_latitude, hub_longitude)
VALUES ('e1000000-0000-0000-0000-000000000001',
        'c1000000-0000-0000-0000-000000000001', 'HN-CENTRAL', 'Ha Noi Central',
        21.028500, 105.854200)
ON CONFLICT (hub_id, zone_code) DO NOTHING;

INSERT INTO shipper_zone_assignment (shipper_id, zone_id)
VALUES ('80000000-0000-0000-0000-000000000007',
        'e1000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

ALTER TABLE pickup_request ADD COLUMN picked_up_at TIMESTAMP;