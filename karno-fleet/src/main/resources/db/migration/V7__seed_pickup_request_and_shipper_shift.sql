INSERT INTO vehicle (id, vehicle_code, license_plate, capacity_weight, capacity_volume, status, created_at, updated_at)
VALUES
    ('d1000000-0000-0000-0000-000000000003', 'V03', '29H-456.67', 100.00, 1.00, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (vehicle_code) DO NOTHING;

INSERT INTO shipper_shift (
    id,
    shipper_user_id,
    vehicle_id,
    hub_id,
    starts_at,
    ends_at,
    status
)
VALUES (
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    '80000000-0000-0000-0000-000000000007',
    'd1000000-0000-0000-0000-000000000003',
    'c1000000-0000-0000-0000-000000000001',
    CURRENT_TIMESTAMP - INTERVAL '1 hour',
    CURRENT_TIMESTAMP + INTERVAL '8 hours',
    'ACTIVE'
);

INSERT INTO pickup_request (
    id,
    request_code,
    shop_name,
    pickup_address,
    pickup_latitude,
    pickup_longitude,
    target_hub_id,
    total_weight,
    total_volume,
    service_duration_seconds,
    pickup_window_start,
    pickup_window_end,
    status,
    created_at,
    updated_at
)
VALUES (
   '10000000-0000-0000-0000-000000000001',
   'DEMO-PU-001',
   'Shop A',
   'Đống Đa, Hà Nội',
   21.018500,
   105.829000,
   'c1000000-0000-0000-0000-000000000001',
   20.00,
   0.10,
   300,
   CURRENT_TIME + INTERVAL '10 minutes',
   CURRENT_TIME + INTERVAL '60 minutes',
   'WAITING_PLANNING',
   CURRENT_TIMESTAMP - INTERVAL '3 hours',
   CURRENT_TIMESTAMP
);

INSERT INTO pickup_request (
    id,
    request_code,
    shop_name,
    pickup_address,
    pickup_latitude,
    pickup_longitude,
    target_hub_id,
    total_weight,
    total_volume,
    service_duration_seconds,
    pickup_window_start,
    pickup_window_end,
    status,
    created_at,
    updated_at
)
VALUES (
   '10000000-0000-0000-0000-000000000002',
   'DEMO-PU-002',
   'Shop B',
   'Ba Đình, Hà Nội',
   21.033500,
   105.814000,
   'c1000000-0000-0000-0000-000000000001',
   15.00,
   0.08,
   300,
   CURRENT_TIME + INTERVAL '15 minutes',
   CURRENT_TIME + INTERVAL '90 minutes',
   'WAITING_PLANNING',
   CURRENT_TIMESTAMP - INTERVAL '2 hours',
   CURRENT_TIMESTAMP
);

INSERT INTO pickup_request (
    id,
    request_code,
    shop_name,
    pickup_address,
    pickup_latitude,
    pickup_longitude,
    target_hub_id,
    total_weight,
    total_volume,
    service_duration_seconds,
    pickup_window_start,
    pickup_window_end,
    status,
    created_at,
    updated_at
)
VALUES (
   '10000000-0000-0000-0000-000000000003',
   'DEMO-PU-003',
   'Shop C',
   'Cầu Giấy, Hà Nội',
   21.036500,
   105.790500,
   'c1000000-0000-0000-0000-000000000001',
   10.00,
   0.05,
   300,
   CURRENT_TIME + INTERVAL '20 minutes',
   CURRENT_TIME + INTERVAL '90 minutes',
   'WAITING_PLANNING',
   CURRENT_TIMESTAMP - INTERVAL '1 hour',
   CURRENT_TIMESTAMP
);

INSERT INTO pickup_request (
    id,
    request_code,
    shop_name,
    pickup_address,
    pickup_latitude,
    pickup_longitude,
    target_hub_id,
    total_weight,
    total_volume,
    service_duration_seconds,
    pickup_window_start,
    pickup_window_end,
    status,
    created_at,
    updated_at
)
VALUES (
   '10000000-0000-0000-0000-000000000004',
   'DEMO-PU-004',
   'Shop D',
   'Hai Bà Trưng, Hà Nội',
   21.010500,
   105.849500,
   'c1000000-0000-0000-0000-000000000001',
   40.00,
   0.20,
   300,
   CURRENT_TIME + INTERVAL '20 minutes',
   CURRENT_TIME + INTERVAL '120 minutes',
   'WAITING_PLANNING',
   CURRENT_TIMESTAMP - INTERVAL '50 minutes',
   CURRENT_TIMESTAMP
);

INSERT INTO pickup_request (
    id,
    request_code,
    shop_name,
    pickup_address,
    pickup_latitude,
    pickup_longitude,
    target_hub_id,
    total_weight,
    total_volume,
    service_duration_seconds,
    pickup_window_start,
    pickup_window_end,
    status,
    created_at,
    updated_at
)
VALUES (
   '10000000-0000-0000-0000-000000000005',
   'DEMO-PU-005',
   'Shop E',
   'Tây Hồ, Hà Nội',
   21.058000,
   105.819000,
   'c1000000-0000-0000-0000-000000000001',
   35.00,
   0.18,
   300,
   CURRENT_TIME + INTERVAL '25 minutes',
   CURRENT_TIME + INTERVAL '135 minutes',
   'WAITING_PLANNING',
   CURRENT_TIMESTAMP - INTERVAL '40 minutes',
   CURRENT_TIMESTAMP
);

INSERT INTO pickup_request (
    id,
    request_code,
    shop_name,
    pickup_address,
    pickup_latitude,
    pickup_longitude,
    target_hub_id,
    total_weight,
    total_volume,
    service_duration_seconds,
    pickup_window_start,
    pickup_window_end,
    status,
    created_at,
    updated_at
)
VALUES (
   '10000000-0000-0000-0000-000000000006',
   'DEMO-PU-006',
   'Shop F - Heavy',
   'Long Biên, Hà Nội',
   21.041000,
   105.886000,
   'c1000000-0000-0000-0000-000000000001',
   120.00,
   0.40,
   600,
   CURRENT_TIME,
   CURRENT_TIME + INTERVAL '150 minutes',
   'WAITING_PLANNING',
   CURRENT_TIMESTAMP - INTERVAL '30 minutes',
   CURRENT_TIMESTAMP
);

INSERT INTO pickup_request (
    id,
    request_code,
    shop_name,
    pickup_address,
    pickup_latitude,
    pickup_longitude,
    target_hub_id,
    total_weight,
    total_volume,
    service_duration_seconds,
    pickup_window_start,
    pickup_window_end,
    status,
    created_at,
    updated_at
)
VALUES (
   '10000000-0000-0000-0000-000000000007',
   'DEMO-PU-007',
   'Shop G - Expired',
   'Thanh Xuân, Hà Nội',
   20.994000,
   105.811000,
   'c1000000-0000-0000-0000-000000000001',
   10.00,
   0.06,
   300,
   CURRENT_TIME - INTERVAL '2 hours',
   CURRENT_TIME - INTERVAL '30 minutes',
   'WAITING_PLANNING',
   CURRENT_TIMESTAMP - INTERVAL '4 hours',
   CURRENT_TIMESTAMP
);

INSERT INTO pickup_request (
    id,
    request_code,
    shop_name,
    pickup_address,
    pickup_latitude,
    pickup_longitude,
    target_hub_id,
    total_weight,
    total_volume,
    service_duration_seconds,
    pickup_window_start,
    pickup_window_end,
    status,
    created_at,
    updated_at
)
VALUES (
    '10000000-0000-0000-0000-000000000008',
    'DEMO-PU-008',
    'Shop H - Future Window',
    'Nam Từ Liêm, Hà Nội',
    21.016000,
    105.765000,
    'c1000000-0000-0000-0000-000000000001',
    10.00,
    0.05,
    300,
    CURRENT_TIME + INTERVAL '2 hours',
    CURRENT_TIME + INTERVAL '3 hours',
    'WAITING_PLANNING',
    CURRENT_TIMESTAMP - INTERVAL '20 minutes',
    CURRENT_TIMESTAMP
);