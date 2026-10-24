-- ========================================================
-- COMMENT ON FOR KARNO_FLEET
-- ========================================================

-- Table: vehicle
COMMENT ON TABLE vehicle IS 'Bảng quản lý phương tiện vận tải (xe tải tuyến cố định linehaul và xe máy/xe tải gom hàng)';
COMMENT ON COLUMN vehicle.id IS 'Khóa chính định danh phương tiện vận tải (UUID)';
COMMENT ON COLUMN vehicle.vehicle_code IS 'Mã định danh phương tiện duy nhất (ví dụ: TRUCK_01, VAN_02)';
COMMENT ON COLUMN vehicle.license_plate IS 'Biển số đăng ký của xe (ví dụ: 29C-12345, 51D-99999)';
COMMENT ON COLUMN vehicle.capacity_weight IS 'Tải trọng tối đa cho phép của xe (đơn vị: kg)';
COMMENT ON COLUMN vehicle.capacity_volume IS 'Thể tích thùng xe tối đa cho phép (đơn vị: m3)';
COMMENT ON COLUMN vehicle.status IS 'Trạng thái hoạt động của xe: AVAILABLE (sẵn sàng), BUSY (đang chạy), MAINTENANCE (bảo trì), INACTIVE (ngừng hoạt động)';
COMMENT ON COLUMN vehicle.created_at IS 'Thời điểm bản ghi xe được tạo trong hệ thống';
COMMENT ON COLUMN vehicle.updated_at IS 'Thời điểm cập nhật thông tin xe gần nhất';

-- Table: linehaul_schedule
COMMENT ON TABLE linehaul_schedule IS 'Bảng lịch trình vận chuyển liên hub (linehaul) định kỳ giữa các hub trung chuyển';
COMMENT ON COLUMN linehaul_schedule.id IS 'Khóa chính định danh lịch trình linehaul (UUID)';
COMMENT ON COLUMN linehaul_schedule.schedule_code IS 'Mã lịch trình linehaul cố định (ví dụ: SCH-HN-DN-01)';
COMMENT ON COLUMN linehaul_schedule.from_hub_id IS 'ID của hub trung chuyển xuất phát';
COMMENT ON COLUMN linehaul_schedule.to_hub_id IS 'ID của hub trung chuyển đích đến';
COMMENT ON COLUMN linehaul_schedule.departure_time IS 'Giờ khởi hành cố định trong ngày của chuyến xe (HH:mm:ss)';
COMMENT ON COLUMN linehaul_schedule.frequency IS 'Tần suất chạy: DAILY (hàng ngày), WEEKLY (hàng tuần), CUSTOM';
COMMENT ON COLUMN linehaul_schedule.planned_duration_minutes IS 'Thời gian di chuyển dự kiến giữa 2 hub (đơn vị: phút)';
COMMENT ON COLUMN linehaul_schedule.capacity_weight IS 'Tải trọng tối đa quy định cho lịch trình này (đơn vị: kg)';
COMMENT ON COLUMN linehaul_schedule.capacity_volume IS 'Thể tích tối đa quy định cho lịch trình này (đơn vị: m3)';
COMMENT ON COLUMN linehaul_schedule.vehicle_id IS 'ID phương tiện mặc định được gán cho lịch trình này (nếu có)';
COMMENT ON COLUMN linehaul_schedule.status IS 'Trạng thái lịch trình: ACTIVE (đang áp dụng), INACTIVE (tạm dừng), CANCELLED (hủy)';
COMMENT ON COLUMN linehaul_schedule.created_at IS 'Thời điểm tạo lịch trình trong hệ thống';
COMMENT ON COLUMN linehaul_schedule.updated_at IS 'Thời điểm cập nhật thông tin lịch trình gần nhất';
COMMENT ON COLUMN linehaul_schedule.planned_distance_km IS 'Khoảng cách tuyến đường di chuyển dự kiến giữa 2 hub (đơn vị: km)';

-- Table: transport_trip
COMMENT ON TABLE transport_trip IS 'Bảng các chuyến xe vận chuyển thực tế được sinh ra từ lịch trình hoặc chuyến đột xuất';
COMMENT ON COLUMN transport_trip.id IS 'Khóa chính định danh chuyến xe thực tế (UUID)';
COMMENT ON COLUMN transport_trip.trip_code IS 'Mã chuyến xe thực tế (ví dụ: TRIP-20261015-HN-DN-01)';
COMMENT ON COLUMN transport_trip.schedule_id IS 'ID lịch trình linehaul mẫu sinh ra chuyến xe này';
COMMENT ON COLUMN transport_trip.from_hub_id IS 'ID của hub xuất phát';
COMMENT ON COLUMN transport_trip.to_hub_id IS 'ID của hub đích';
COMMENT ON COLUMN transport_trip.vehicle_id IS 'ID phương tiện thực tế được phân công chạy chuyến này';
COMMENT ON COLUMN transport_trip.scheduled_departure_at IS 'Thời gian khởi hành theo kế hoạch';
COMMENT ON COLUMN transport_trip.planned_arrival_at IS 'Thời gian đến nơi dự kiến theo kế hoạch ban đầu';
COMMENT ON COLUMN transport_trip.estimated_arrival_at IS 'Thời gian đến nơi ước tính hiện tại (cập nhật theo thời gian thực)';
COMMENT ON COLUMN transport_trip.actual_departure_at IS 'Thời gian xe thực tế rời hub xuất phát';
COMMENT ON COLUMN transport_trip.actual_arrival_at IS 'Thời gian xe thực tế đến và cập bến hub đích';
COMMENT ON COLUMN transport_trip.capacity_weight IS 'Tải trọng tối đa của chuyến xe (đơn vị: kg)';
COMMENT ON COLUMN transport_trip.reserved_weight IS 'Khối lượng hàng hóa đã xếp hoặc giữ chỗ trên chuyến xe (đơn vị: kg)';
COMMENT ON COLUMN transport_trip.status IS 'Trạng thái chuyến xe: SCHEDULED (đã lên lịch), BOARDING (đang xếp hàng), DEPARTED (đã xuất phát), IN_TRANSIT (đang di chuyển), ARRIVED (đã đến), CANCELLED (hủy chuyến)';
COMMENT ON COLUMN transport_trip.created_at IS 'Thời điểm khởi tạo chuyến xe';
COMMENT ON COLUMN transport_trip.updated_at IS 'Thời điểm cập nhật thông tin chuyến xe gần nhất';
COMMENT ON COLUMN transport_trip.planned_distance_km IS 'Khoảng cách lộ trình dự kiến của chuyến xe (đơn vị: km)';

-- Table: shipper_shift
COMMENT ON TABLE shipper_shift IS 'Bảng quản lý ca làm việc của tài xế/shipper lấy hàng và giao hàng chặng đầu/chặng cuối';
COMMENT ON COLUMN shipper_shift.id IS 'Khóa chính định danh ca làm việc shipper (UUID)';
COMMENT ON COLUMN shipper_shift.shipper_user_id IS 'ID tài khoản người dùng của tài xế/shipper';
COMMENT ON COLUMN shipper_shift.vehicle_id IS 'ID phương tiện giao cho shipper sử dụng trong ca làm việc';
COMMENT ON COLUMN shipper_shift.hub_id IS 'ID hub trực thuộc mà shipper đăng ký nhận ca làm việc';
COMMENT ON COLUMN shipper_shift.starts_at IS 'Thời gian bắt đầu ca làm việc';
COMMENT ON COLUMN shipper_shift.ends_at IS 'Thời gian kết thúc ca làm việc';
COMMENT ON COLUMN shipper_shift.status IS 'Trạng thái ca làm việc: SCHEDULED (đã phân ca), ACTIVE (đang làm việc), COMPLETED (hoàn thành), CANCELLED (hủy ca)';
COMMENT ON COLUMN shipper_shift.created_at IS 'Thời điểm tạo ca làm việc';
COMMENT ON COLUMN shipper_shift.updated_at IS 'Thời điểm cập nhật ca làm việc gần nhất';

-- Table: pickup_request
COMMENT ON TABLE pickup_request IS 'Bảng yêu cầu tài xế/shipper đến địa chỉ người bán/shop để lấy hàng (First-Mile Pickup)';
COMMENT ON COLUMN pickup_request.id IS 'Khóa chính định danh yêu cầu lấy hàng (UUID)';
COMMENT ON COLUMN pickup_request.request_code IS 'Mã yêu cầu lấy hàng (ví dụ: PICKUP-20261015-001)';
COMMENT ON COLUMN pickup_request.shop_name IS 'Tên cửa hàng hoặc người gửi hàng cần lấy';
COMMENT ON COLUMN pickup_request.pickup_address IS 'Địa chỉ chi tiết nơi tài xế đến lấy hàng';
COMMENT ON COLUMN pickup_request.pickup_latitude IS 'Tọa độ vĩ độ (Latitude) điểm lấy hàng phục vụ định vị và định tuyến TSP';
COMMENT ON COLUMN pickup_request.pickup_longitude IS 'Tọa độ kinh độ (Longitude) điểm lấy hàng phục vụ định vị và định tuyến TSP';
COMMENT ON COLUMN pickup_request.target_hub_id IS 'ID hub tiếp nhận gom hàng sau khi shipper lấy hàng từ shop về';
COMMENT ON COLUMN pickup_request.total_weight IS 'Tổng khối lượng ước tính của các kiện hàng cần lấy (đơn vị: kg)';
COMMENT ON COLUMN pickup_request.total_volume IS 'Tổng thể tích ước tính của các kiện hàng (đơn vị: m3)';
COMMENT ON COLUMN pickup_request.service_duration_seconds IS 'Thời gian dự kiến shipper dừng bốc dỡ hàng tại điểm lấy (đơn vị: giây)';
COMMENT ON COLUMN pickup_request.pickup_window_start IS 'Khung giờ sớm nhất người gửi có thể bàn giao hàng cho shipper';
COMMENT ON COLUMN pickup_request.pickup_window_end IS 'Khung giờ muộn nhất người gửi yêu cầu shipper phải đến lấy';
COMMENT ON COLUMN pickup_request.status IS 'Trạng thái yêu cầu lấy hàng: PENDING (chờ phân công), ASSIGNED (đã gán shipper), IN_PROGRESS (đang đi lấy), COMPLETED (đã lấy hàng thành công), FAILED (lấy hàng thất bại), CANCELLED (đã hủy)';
COMMENT ON COLUMN pickup_request.created_at IS 'Thời điểm phát sinh yêu cầu lấy hàng';
COMMENT ON COLUMN pickup_request.updated_at IS 'Thời điểm cập nhật thông tin yêu cầu lấy hàng gần nhất';
