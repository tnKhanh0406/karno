ALTER TABLE pickup_request
    ADD COLUMN shop_id UUID,
    ADD COLUMN seller_id UUID;

UPDATE pickup_request SET status = 'OPEN' WHERE status = 'WAITING_PLANNING';

CREATE TABLE pickup_request_shipment (
    pickup_request_id UUID NOT NULL REFERENCES pickup_request(id) ON DELETE CASCADE,
    shipment_id UUID NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (pickup_request_id, shipment_id),
    UNIQUE (shipment_id)
);

CREATE INDEX idx_pickup_request_shop_status
    ON pickup_request (shop_id, status, created_at);
