ALTER TABLE shipments
    ADD COLUMN pickup_request_id UUID,
    ADD COLUMN total_volume_m3 DECIMAL(12,6) NOT NULL DEFAULT 0;

CREATE INDEX idx_shipments_pickup_request_id
    ON shipments (pickup_request_id)
    WHERE pickup_request_id IS NOT NULL;
    
CREATE INDEX idx_shipments_shop_status_created
    ON shipments (shop_id, status, created_at);