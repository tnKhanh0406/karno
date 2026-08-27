-- Table: categories
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    parent_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    image_url TEXT
);

-- Table: products
CREATE TABLE products (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL,
    category_id UUID REFERENCES categories(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- DRAFT, ACTIVE, INACTIVE, DELETED
    sold_count INT DEFAULT 0,
    rating DECIMAL(15, 2) DEFAULT 0.00,
    thumbnail_img VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: product_variants
CREATE TABLE product_variants (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(100) UNIQUE,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(15, 2) NOT NULL,
    primary_img VARCHAR(255),
    weight_gram INT DEFAULT 0,
    length_cm DECIMAL(10, 2) DEFAULT 0,
    width_cm DECIMAL(10, 2) DEFAULT 0,
    height_cm DECIMAL(10, 2) DEFAULT 0
);

-- Table: product_attributes
CREATE TABLE product_attributes (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL
);

-- Table: product_attribute_values
CREATE TABLE product_attribute_values (
    id UUID PRIMARY KEY,
    attribute_id UUID NOT NULL REFERENCES product_attributes(id) ON DELETE CASCADE,
    value VARCHAR(100) NOT NULL
);

-- Table: variant_attribute_value
CREATE TABLE variant_attribute_value (
    id UUID PRIMARY KEY,
    attribute_value_id UUID NOT NULL REFERENCES product_attribute_values(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE
);

-- Table: product_images
CREATE TABLE product_images (
    id UUID PRIMARY KEY,
    image_type VARCHAR(100) NOT NULL, -- PRODUCT, VARIANT
    image_url TEXT NOT NULL,
    product_id UUID REFERENCES products(id) ON DELETE CASCADE,
    variant_id UUID REFERENCES product_variants(id) ON DELETE CASCADE
);

-- Table: product_reviews
CREATE TABLE product_reviews (
    id UUID PRIMARY KEY,
    comment VARCHAR(500),
    rating INT CHECK (rating >= 1 AND rating <= 5),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    shop_id UUID NOT NULL,
    user_id UUID NOT NULL,
    order_item_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE categories IS 'Danh mục sản phẩm (Hỗ trợ cấu trúc phân cấp danh mục cha - con)';
COMMENT ON TABLE products IS 'Bảng lưu trữ thông tin sản phẩm chính';
COMMENT ON TABLE product_variants IS 'Bảng lưu các biến thể của sản phẩm';
COMMENT ON TABLE product_attributes IS 'Bảng lưu các thuộc tính sản phẩm do Shop tự định nghĩa (Màu sắc, Size)';
COMMENT ON TABLE product_attribute_values IS 'Bảng lưu giá trị cụ thể của thuộc tính (Đỏ, Xanh, L, XL)';
COMMENT ON TABLE variant_attribute_value IS 'Bảng liên kết nhiều - nhiều giữa biến thể sản phẩm và giá trị thuộc tính';
COMMENT ON TABLE product_images IS 'Bảng quản lý hình ảnh của sản phẩm và biến thể';
COMMENT ON TABLE product_reviews IS 'Bảng lưu đánh giá và bình luận của khách hàng sau khi mua hàng';

COMMENT ON COLUMN categories.parent_id IS 'Xác định danh mục cha của danh mục hiện tại, null thì không có cha'
COMMENT ON COLUMN products.status IS 'Trạng thái sản phẩm: DRAFT (Nháp), ACTIVE (Đang bán), INACTIVE (Ngừng bán), DELETED (Đã xóa)';
COMMENT ON COLUMN products.thumbnail_img IS 'Snapshot ảnh chính của product'
COMMENT ON COLUMN product_variants.weight_gram IS 'Khối lượng đóng gói tính theo Gram';