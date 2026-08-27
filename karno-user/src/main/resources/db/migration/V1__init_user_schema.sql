-- Table: users
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    avatar_url TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: user_addresses
CREATE TABLE user_addresses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_name VARCHAR(255) NOT NULL,
    receiver_address VARCHAR(500) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: shops
CREATE TABLE shops (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    logo_url TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    address TEXT NOT NULL,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE users IS 'Bảng lưu trữ thông tin tài khoản người dùng (User, Seller, Admin)';
COMMENT ON TABLE user_addresses IS 'Bảng lưu địa chỉ nhận hàng của người dùng';
COMMENT ON TABLE shops IS 'Bảng lưu trữ thông tin Cửa hàng của Người bán';

COMMENT ON COLUMN users.role IS 'Vai trò tài khoản: ADMIN, USER, SELLER';
COMMENT ON COLUMN user_addresses.is_default IS 'Xác định địa chỉ mặc định của khách hàng';
COMMENT ON COLUMN user_addresses.latitude IS 'Tọa độ vĩ độ địa chỉ khách hàng (Dùng tính phí giao hàng & định vị)';
COMMENT ON COLUMN user_addresses.longitude IS 'Tọa độ kinh độ địa chỉ khách hàng (Dùng tính phí giao hàng & định vị)';
COMMENT ON COLUMN shops.latitude IS 'Tọa độ vĩ độ Cửa hàng (Dùng cho Shipper tìm điểm lấy hàng)';
COMMENT ON COLUMN shops.longitude IS 'Tọa độ kinh độ Cửa hàng (Dùng cho Shipper tìm điểm lấy hàng)';