-- Table: carts
CREATE TABLE carts (
    id UUID PRIMARY KEY DEFAULT,
    customer_id UUID NOT NULL UNIQUE
);

-- Table: cart_items
CREATE TABLE cart_items (
    id UUID PRIMARY KEY DEFAULT,
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL,
    product_id UUID NOT NULL,
    shop_id UUID NOT NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0)
);

-- Table: orders
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT,
    customer_id UUID NOT NULL,
    receiver_name VARCHAR(255) NOT NULL,
    receiver_address VARCHAR(500) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    shop_id UUID NOT NULL,
    shop_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    subtotal DECIMAL(15, 2) NOT NULL,
    shipping_fee DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(15, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL, -- COD, ONLINE
    payment_status VARCHAR(30) NOT NULL,
    delivery_slot_start TIMESTAMP WITH TIME ZONE,
    delivery_slot_end TIMESTAMP WITH TIME ZONE,
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: order_items
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    variant_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    variant_name VARCHAR(255) NOT NULL,
    sku VARCHAR(100),
    product_image_url TEXT,
    unit_price DECIMAL(15, 2) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    subtotal DECIMAL(15, 2) NOT NULL,
    weight_gram INT DEFAULT 0,
    length_cm DECIMAL(10, 2) DEFAULT 0,
    width_cm DECIMAL(10, 2) DEFAULT 0,
    height_cm DECIMAL(10, 2) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: order_status_histories
CREATE TABLE order_status_histories (
    id UUID PRIMARY KEY DEFAULT,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    actor_type VARCHAR(50) NOT NULL, -- CUSTOMER, SELLER, SHIPPER, HUB_STAFF, ADMIN, SYSTEM
    actor_id UUID,
    description TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);