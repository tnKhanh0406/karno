-- ========================================================
-- Migration V2: Thêm COMMENT ON cho các bảng và cột của karno-user
-- ========================================================

-- Table: users
COMMENT ON TABLE users IS 'Bảng lưu trữ thông tin tài khoản người dùng (User, Seller, Shipper, Admin)';
COMMENT ON COLUMN users.id IS 'Khóa chính định danh tài khoản người dùng (UUID)';
COMMENT ON COLUMN users.username IS 'Tên đăng nhập duy nhất của người dùng trong hệ thống';
COMMENT ON COLUMN users.email IS 'Địa chỉ thư điện tử (email) của người dùng';
COMMENT ON COLUMN users.phone IS 'Số điện thoại liên hệ chính của người dùng';
COMMENT ON COLUMN users.password IS 'Mật khẩu đăng nhập đã được mã hóa (BCrypt hash)';
COMMENT ON COLUMN users.full_name IS 'Họ và tên đầy đủ của người dùng';
COMMENT ON COLUMN users.avatar_url IS 'Đường dẫn URL ảnh đại diện của người dùng';
COMMENT ON COLUMN users.status IS 'Trạng thái tài khoản: ACTIVE (hoạt động), LOCKED (bị khóa), INACTIVE (chưa kích hoạt)';
COMMENT ON COLUMN users.role IS 'Vai trò quyền hạn người dùng: CUSTOMER, SELLER, SHIPPER, ADMIN';
COMMENT ON COLUMN users.created_at IS 'Thời điểm khởi tạo tài khoản';
COMMENT ON COLUMN users.updated_at IS 'Thời điểm cập nhật thông tin tài khoản gần nhất';

-- Table: shops
COMMENT ON TABLE shops IS 'Bảng lưu trữ thông tin Cửa hàng / Gian hàng của Người bán (Seller)';
COMMENT ON COLUMN shops.id IS 'Khóa chính định danh gian hàng (UUID)';
COMMENT ON COLUMN shops.owner_id IS 'ID tài khoản người dùng sở hữu cửa hàng (liên kết bảng users)';
COMMENT ON COLUMN shops.name IS 'Tên hiển thị công khai của gian hàng';
COMMENT ON COLUMN shops.description IS 'Mô tả chi tiết và giới thiệu về gian hàng';
COMMENT ON COLUMN shops.logo_url IS 'Đường dẫn URL logo đại diện của gian hàng';
COMMENT ON COLUMN shops.status IS 'Trạng thái gian hàng: ACTIVE (đang mở bán), CLOSED (đóng cửa), BANNED (bị cấm)';
COMMENT ON COLUMN shops.address IS 'Địa chỉ văn phòng / kho hàng mặc định của shop';
COMMENT ON COLUMN shops.latitude IS 'Tọa độ vĩ độ kho shop phục vụ tính cước và điều phối lấy hàng';
COMMENT ON COLUMN shops.longitude IS 'Tọa độ kinh độ kho shop phục vụ tính cước và điều phối lấy hàng';
COMMENT ON COLUMN shops.created_at IS 'Thời điểm đăng ký gian hàng';
COMMENT ON COLUMN shops.updated_at IS 'Thời điểm cập nhật thông tin gian hàng gần nhất';

-- Table: user_addresses
COMMENT ON TABLE user_addresses IS 'Bảng lưu sổ địa chỉ nhận hàng của người dùng (Customer Shipping Addresses)';
COMMENT ON COLUMN user_addresses.id IS 'Khóa chính định danh địa chỉ nhận hàng (UUID)';
COMMENT ON COLUMN user_addresses.user_id IS 'ID tài khoản người dùng sở hữu địa chỉ (liên kết bảng users)';
COMMENT ON COLUMN user_addresses.receiver_name IS 'Tên người nhận hàng tại địa chỉ này';
COMMENT ON COLUMN user_addresses.receiver_address IS 'Địa chỉ chi tiết nơi giao hàng (số nhà, đường, phường/xã, quận/huyện, tỉnh/tp)';
COMMENT ON COLUMN user_addresses.receiver_phone IS 'Số điện thoại liên hệ của người nhận';
COMMENT ON COLUMN user_addresses.is_default IS 'Đánh dấu địa chỉ giao hàng mặc định của người dùng (true/false)';
COMMENT ON COLUMN user_addresses.latitude IS 'Tọa độ vĩ độ của địa chỉ phục vụ định vị và tính cước vận chuyển';
COMMENT ON COLUMN user_addresses.longitude IS 'Tọa độ kinh độ của địa chỉ phục vụ định vị và tính cước vận chuyển';
COMMENT ON COLUMN user_addresses.created_at IS 'Thời điểm lưu địa chỉ vào sổ địa chỉ';
