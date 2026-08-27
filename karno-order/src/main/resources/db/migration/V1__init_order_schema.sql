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

COMMENT ON TABLE carts IS 'Bảng giỏ hàng cá nhân của người mua';
COMMENT ON TABLE cart_items IS 'Bảng lưu các sản phẩm chi tiết có trong giỏ hàng';
COMMENT ON TABLE orders IS 'Bảng lưu trữ thông tin đơn hàng chính thức do khách đặt';
COMMENT ON TABLE order_items IS 'Bảng lưu chi tiết các sản phẩm trong đơn hàng (Lưu dạng Snapshot dữ liệu tại thời điểm đặt)';
COMMENT ON TABLE order_status_histories IS 'Bảng lịch sử vết chuyển đổi trạng thái của đơn hàng (Timeline Audit Log)';

COMMENT ON COLUMN orders.payment_method IS 'Phương thức thanh toán: COD (Tiền mặt), ONLINE (Thanh toán trực tuyến)';
COMMENT ON COLUMN orders.delivery_slot_start IS 'Khung giờ sớm nhất khách rảnh để nhận hàng (Nếu chọn khung giờ 18h-21h thì start sẽ là 18h)';
COMMENT ON COLUMN orders.delivery_slot_end IS 'Khung giờ muộn nhất khách có thể nhận hàng (Nếu chọn khung giờ 18h-21h thì end sẽ là 21h)';
COMMENT ON COLUMN order_status_histories.actor_type IS 'Đối tượng thực hiện thay đổi: CUSTOMER, SELLER, SHIPPER, HUB_STAFF, ADMIN, SYSTEM';