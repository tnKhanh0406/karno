CREATE TABLE shipments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    checkout_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    shop_id UUID NOT NULL,
    status VARCHAR(40) NOT NULL,
    shipping_method VARCHAR(50) NOT NULL,
    shipping_fee DECIMAL(15,2) NOT NULL,
    total_weight_gram INT NOT NULL,
    package_payload JSONB NOT NULL,
    -- Pickup snapshot
    pickup_phone VARCHAR(20),
    pickup_address VARCHAR(500) NOT NULL,
    pickup_latitude DECIMAL(9,6),
    pickup_longitude DECIMAL(10,6),
    -- Dropoff snapshot
    receiver_name VARCHAR(255) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    receiver_address VARCHAR(500) NOT NULL,
    receiver_latitude DECIMAL(9,6),
    receiver_longitude DECIMAL(10,6),

    estimated_delivery_at TIMESTAMP WITH TIME ZONE,

    picked_up_at TIMESTAMP WITH TIME ZONE,

    delivered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);