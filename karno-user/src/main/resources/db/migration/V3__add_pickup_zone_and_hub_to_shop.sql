ALTER TABLE shops ADD COLUMN pickup_zone_id UUID;
CREATE INDEX idx_shops_pickup_zone_id ON shops (pickup_zone_id);
ALTER TABLE shops ADD COLUMN origin_hub_id UUID;
CREATE INDEX idx_shops_origin_hub_id ON shops (origin_hub_id);