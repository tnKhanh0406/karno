-- Table: inventories
CREATE TABLE inventories (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL,
    variant_id UUID NOT NULL UNIQUE,
    on_hand_quantity INT NOT NULL DEFAULT 0 CHECK (on_hand_quantity >= 0),
    reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: inventory_reservations
CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY,
    inventory_id UUID NOT NULL REFERENCES inventories(id) ON DELETE CASCADE,
    order_id UUID NOT NULL,
    order_item_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, CONFIRMED, RELEASED, EXPIRED
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    released_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: inventory_movements
CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,
    inventory_id UUID NOT NULL REFERENCES inventories(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL,
    quantity INT NOT NULL,
    on_hand_before INT NOT NULL,
    on_hand_after INT NOT NULL,
    reserved_before INT NOT NULL,
    reserved_after INT NOT NULL,
    reference_type VARCHAR(40) NOT NULL,
    reference_id UUID NOT NULL,
    reason TEXT,
    actor_type VARCHAR(30) NOT NULL,
    actor_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE inventories IS 'Bảng quản lý tồn kho thực tế và tồn kho tạm giữ của từng biến thể sản phẩm';
COMMENT ON TABLE inventory_reservations IS 'Bảng quản lý việc tạm giữ hàng tồn kho khi khách tạo đơn hàng/checkout';
COMMENT ON TABLE inventory_movements IS 'Bảng lưu lịch sử biến động tồn kho (Audit Log nhập/xuất/giữ/giải phóng hàng)';

COMMENT ON COLUMN inventories.on_hand_quantity IS 'Tổng số lượng hàng thực tế hiện có trong kho';
COMMENT ON COLUMN inventories.reserved_quantity IS 'Số lượng hàng đang bị tạm giữ cho các đơn hàng chưa hoàn tất';
COMMENT ON COLUMN inventory_reservations.status IS 'Trạng thái giữ hàng: ACTIVE (Đang giữ), CONFIRMED (Đã bán/trừ kho), RELEASED (Đã hủy giữ), EXPIRED (Hết hạn giữ)';
COMMENT ON COLUMN inventory_reservations.expires_at IS 'Thời điểm giữ hàng tự động hết hạn nếu khách không hoàn tất checkout/thanh toán';
COMMENT ON COLUMN inventory_movements.reference_type IS 'Loại tham chiếu liên quan đến biến động tồn kho: ORDER, PURCHASE, ADJUSTMENT, RETURN';
COMMENT ON COLUMN inventory_movements.reference_id IS 'ID tham chiếu liên quan đến biến động tồn kho (ví dụ: ID đơn hàng, ID phiếu nhập kho, ID điều chỉnh tồn kho, ID trả hàng)';
COMMENT ON COLUMN inventory_movements.actor_type IS 'Loại thực thể thực hiện biến động tồn kho: SYSTEM, USER, SELLER, ADMIN';
COMMENT ON COLUMN inventory_movements.actor_id IS 'ID thực thể thực hiện biến động tồn kho (ví dụ: ID người dùng, ID seller, ID admin)';