-- Migration V5: Bổ sung planned_distance_km cho linehaul_schedule và transport_trip; làm giàu phương tiện và mạng lưới lịch trình

ALTER TABLE linehaul_schedule ADD COLUMN IF NOT EXISTS planned_distance_km DECIMAL(10, 2);
ALTER TABLE transport_trip ADD COLUMN IF NOT EXISTS planned_distance_km DECIMAL(10, 2);

-- Cập nhật cự ly cho 5 lịch trình hiện có
UPDATE linehaul_schedule SET planned_distance_km = 1720.00 WHERE schedule_code = 'S-HN-HCM-2200';
UPDATE linehaul_schedule SET planned_distance_km = 170.00  WHERE schedule_code = 'S-HCM-CT-0800';
UPDATE linehaul_schedule SET planned_distance_km = 135.00  WHERE schedule_code = 'S-HCM-VL-0600';
UPDATE linehaul_schedule SET planned_distance_km = 35.00   WHERE schedule_code = 'S-VL-CT-0930';
UPDATE linehaul_schedule SET planned_distance_km = 980.00  WHERE schedule_code = 'S-DN-CT-2100';

-- Làm giàu thêm đội xe (Vehicles)
INSERT INTO vehicle (id, vehicle_code, license_plate, capacity_weight, capacity_volume, status, created_at, updated_at)
VALUES
    ('d1000000-0000-0000-0000-000000000003', 'V03', '43C-789.01', 10000.00, 50.00, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d1000000-0000-0000-0000-000000000004', 'V04', '65C-234.56', 5000.00,  25.00, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('d1000000-0000-0000-0000-000000000005', 'V05', '51D-345.67', 8000.00,  40.00, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (vehicle_code) DO NOTHING;

-- Làm giàu thêm các lịch trình Linehaul thực tế (đa dạng cung đường, 2 chiều, cao tốc)
INSERT INTO linehaul_schedule (
    id, schedule_code, from_hub_id, to_hub_id, departure_time, frequency,
    planned_duration_minutes, planned_distance_km, capacity_weight, capacity_volume, vehicle_id, status, created_at, updated_at
) VALUES
    -- 1. Tuyến Tốc hành Bắc - Nam chạy ngày: HN01 -> HCM01 (09:00, chạy 28 giờ = 1680 phút, cự ly cao tốc 1680 km)
    ('d2000000-0000-0000-0000-000000000006', 'S-HN-HCM-0900',
     'c1000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000002',
     '09:00:00', 'DAILY', 1680, 1680.00, 8000.00, 40.00,
     'd1000000-0000-0000-0000-000000000002', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- 2. Tuyến Miền Trung -> Miền Nam: DN01 -> HCM01 (14:00, chạy 14 giờ = 840 phút, cự ly 850 km)
    ('d2000000-0000-0000-0000-000000000007', 'S-DN-HCM-1400',
     'c1000000-0000-0000-0000-000000000003', 'c1000000-0000-0000-0000-000000000002',
     '14:00:00', 'DAILY', 840, 850.00, 10000.00, 50.00,
     'd1000000-0000-0000-0000-000000000003', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- 3. Chiều về Nam -> Bắc: HCM01 -> HN01 (20:00, chạy 30 giờ = 1800 phút, cự ly 1720 km)
    ('d2000000-0000-0000-0000-000000000008', 'S-HCM-HN-2000',
     'c1000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000001',
     '20:00:00', 'DAILY', 1800, 1720.00, 8000.00, 40.00,
     'd1000000-0000-0000-0000-000000000005', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- 4. Chiều về Tây Nam Bộ -> TP.HCM: CT01 -> HCM01 (14:00, chạy 4 giờ = 240 phút, cự ly 170 km)
    ('d2000000-0000-0000-0000-000000000009', 'S-CT-HCM-1400',
     'c1000000-0000-0000-0000-000000000004', 'c1000000-0000-0000-0000-000000000002',
     '14:00:00', 'DAILY', 240, 170.00, 5000.00, 25.00,
     'd1000000-0000-0000-0000-000000000004', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- 5. Tuyến tối tăng cường TP.HCM -> Cần Thơ: HCM01 -> CT01 (19:00, chạy 4 giờ = 240 phút, cự ly 170 km)
    ('d2000000-0000-0000-0000-000000000010', 'S-HCM-CT-1900',
     'c1000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000004',
     '19:00:00', 'DAILY', 240, 170.00, 8000.00, 40.00,
     'd1000000-0000-0000-0000-000000000005', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (schedule_code) DO NOTHING;
