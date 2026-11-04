ALTER TABLE pickup_request
    ALTER COLUMN total_volume TYPE DECIMAL(12,6);

-- A COMPLETING session still owns its shipper and vehicle until all local and
-- Shipping state transitions have converged.
DROP INDEX IF EXISTS uk_active_pickup_session_shipper;
DROP INDEX IF EXISTS uk_active_pickup_session_vehicle;

CREATE UNIQUE INDEX uk_current_pickup_session_shipper
    ON pickup_session (shipper_id)
    WHERE status IN ('ACTIVE', 'COMPLETING');

CREATE UNIQUE INDEX uk_current_pickup_session_vehicle
    ON pickup_session (vehicle_id)
    WHERE status IN ('ACTIVE', 'COMPLETING');

CREATE INDEX idx_pickup_request_session_status
    ON pickup_request (pickup_session_id, status);

CREATE TABLE pickup_session_deferred_request (
    pickup_session_id UUID NOT NULL REFERENCES pickup_session(id) ON DELETE CASCADE,
    pickup_request_id UUID NOT NULL REFERENCES pickup_request(id) ON DELETE CASCADE,
    deferred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (pickup_session_id, pickup_request_id)
);
