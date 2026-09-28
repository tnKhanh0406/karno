# Hub-to-Hub — Task 1: Network & Linehaul Foundation

## 1. Mục tiêu

Task 1 chuẩn bị dữ liệu nền và cơ chế sinh chuyến để bài toán Hub-to-Hub có dữ liệu thật để chạy.

Flow:

```text
Hub
+
HubConnection
+
Vehicle
+
LinehaulSchedule
        ↓
Trip Generator
        ↓
TransportTrip
        ↓
Task 2: Time-dependent Dijkstra
```

Không hardcode danh sách `TransportTrip` cụ thể.

---

## 2. Scope Task 1

### Làm

- Thiết kế DB cho `Hub`
- Thiết kế DB cho `HubConnection`
- Thiết kế DB cho `Vehicle`
- Thiết kế DB cho `LinehaulSchedule`
- Thiết kế DB cho `TransportTrip`
- Seed dữ liệu cấu hình ban đầu bằng Flyway/SQL
- Implement `TripGeneratorService`
- Implement Scheduler để tự sinh các `TransportTrip` sắp tới
- Đảm bảo Trip Generator idempotent

### Chưa làm

- Admin CRUD UI
- Dijkstra
- Parcel routing
- Reservation
- GPS
- Traffic
- ETA monitoring
- Replanning
- Hub Receiving/Sorting/Loading UI

---

# 3. Service ownership

## `karno-hub`

Quản lý mạng Hub:

```text
Hub
HubConnection
```

## `karno-fleet`

Quản lý phương tiện và chuyến:

```text
Vehicle
LinehaulSchedule
TransportTrip
```

Không tạo foreign key trực tiếp giữa database của các microservice.

---

# 4. Database design

## 4.1. `hub`

Mục đích: lưu các Hub trong mạng Karno.

```text
hub
---
id
hub_code
name
min_transfer_minutes
status
created_at
updated_at
```

Ví dụ:

```text
HN01  | Hà Nội Hub | 120 | ACTIVE
HCM01 | Hồ Chí Minh Hub | 120 | ACTIVE
DN01  | Đà Nẵng Hub | 120 | ACTIVE
CT01  | Cần Thơ Hub | 120 | ACTIVE
VL01  | Vĩnh Long Hub | 60 | ACTIVE
```

### `min_transfer_minutes`

Là thời gian tối thiểu để Parcel được xử lý tại Hub trước khi có thể đi tiếp.

Ví dụ:

```text
Parcel đến HCM01 lúc 23:00
+
minTransferTime = 120 phút
=
readyAt tiếp theo = 01:00
```

---

# 5. `hub_connection`

Mục đích: mô tả các kết nối vận chuyển trực tiếp giữa Hub.

```text
hub_connection
--------------
id
from_hub_id
to_hub_id
status
created_at
updated_at
```

Ví dụ:

```text
HN01  → HCM01
HN01  → DN01
HCM01 → CT01
HCM01 → VL01
VL01  → CT01
DN01  → CT01
```

Ý nghĩa:

> `HubConnection` chỉ nói rằng mạng vận tải cho phép đi trực tiếp từ Hub A tới Hub B.

Không lưu lịch giờ chạy ở đây.

---

# 6. `vehicle`

Mục đích: lưu phương tiện có thể thực hiện `TransportTrip`.

```text
vehicle
-------
id
vehicle_code
license_plate
capacity_weight
capacity_volume
status
created_at
updated_at
```

Ví dụ:

```text
V01 | 29H-123.45 | 5000 kg | AVAILABLE
V02 | 30H-456.78 | 8000 kg | AVAILABLE
```

Biển số thuộc `Vehicle`, không thuộc `LinehaulSchedule`.

---

# 7. `linehaul_schedule`

Đây là **lịch chạy**, không phải một chuyến cụ thể.

Ví dụ:

```text
HN01 → HCM01
22:00 mỗi ngày
```

Schema:

```text
linehaul_schedule
------------------
id
schedule_code
from_hub_id
to_hub_id
departure_time
frequency
planned_duration_minutes
capacity_weight
capacity_volume
vehicle_id
status
created_at
updated_at
```

Ví dụ:

```text
S-HN-HCM-2200
HN01 → HCM01
departure = 22:00
frequency = DAILY
plannedDuration = 480 phút
capacity = 5000 kg
vehicle = V01
status = ACTIVE
```

`planned_duration_minutes` là **thời gian dự kiến** dùng để lập lịch ban đầu, không phải actual duration.

---

# 8. `transport_trip`

Đây là **chuyến cụ thể** được tạo từ `LinehaulSchedule`.

Schema:

```text
transport_trip
--------------
id
trip_code
schedule_id
from_hub_id
to_hub_id
vehicle_id
scheduled_departure_at
planned_arrival_at
estimated_arrival_at
actual_departure_at
actual_arrival_at
capacity_weight
reserved_weight
status
created_at
updated_at
```

Ví dụ:

```text
OB-20260921-001
HN01 → HCM01

scheduledDepartureAt = 2026-09-21 22:00
plannedArrivalAt     = 2026-09-22 06:00
estimatedArrivalAt   = 2026-09-22 06:00
vehicle              = V01
status               = PLANNED
```

### Ý nghĩa các trường thời gian

```text
scheduled_departure_at
→ lịch xe dự kiến rời Hub

planned_arrival_at
→ thời gian đến dự kiến khi tạo Trip

estimated_arrival_at
→ ETA mới nhất trong quá trình vận hành

actual_departure_at
→ thời điểm xe thực tế rời Hub

actual_arrival_at
→ thời điểm xe thực tế tới Hub
```

---

# 9. Trip Generator

## 9.1. Mục đích

Không hardcode:

```text
OB-001
OB-002
OB-003
...
```

Thay vào đó:

```text
LinehaulSchedule
"HN01 → HCM01, 22:00 DAILY"
        ↓
Trip Generator
        ↓
TransportTrip cụ thể theo ngày
```

Ví dụ:

```text
Schedule:
HN01 → HCM01
22:00 DAILY
plannedDuration = 8h
```

Generator tạo:

```text
OB-001
21/09 22:00 → 22/09 06:00

OB-002
22/09 22:00 → 23/09 06:00

OB-003
23/09 22:00 → 24/09 06:00
```

---

# 10. Generation horizon

Không chỉ tạo đúng một chuyến tiếp theo.

Ví dụ:

```text
generationHorizon = 7 ngày
```

Nếu hôm nay là `21/09`:

```text
21/09 → OB-001
22/09 → OB-002
23/09 → OB-003
24/09 → OB-004
25/09 → OB-005
26/09 → OB-006
27/09 → OB-007
```

Mục đích:

> Luôn có sẵn các TransportTrip tương lai để Route Planning có thể xét.

---

# 11. Trip Generator input

```text
LinehaulSchedule
+
currentTime
+
generationHorizon
```

Ví dụ:

```text
schedule:
HN01 → HCM01
22:00 DAILY

now:
2026-09-21 10:00

horizon:
7 days
```

Output:

```text
7 TransportTrip
```

---

# 12. Trip Generator logic

Pseudo flow:

```text
1. Load ACTIVE LinehaulSchedule
2. Tính các departure time trong generation horizon
3. Với mỗi departure:
   - kiểm tra TransportTrip đã tồn tại chưa
   - nếu chưa:
       tạo TransportTrip
   - nếu đã:
       bỏ qua
4. Lưu batch
```

Ví dụ:

```text
S01
HN01 → HCM01
22:00 DAILY

↓

21/09 22:00
22/09 22:00
23/09 22:00
...
27/09 22:00

↓

find existing trip

↓

chỉ insert trip chưa có
```

---

# 13. Idempotency

Đây là bắt buộc.

Job có thể chạy lại mà không được tạo duplicate.

Ví dụ:

```text
(schedule_id = S01, departure = 2026-09-21 22:00)
```

chỉ được có **một** TransportTrip.

Nên tạo unique constraint:

```text
UNIQUE(schedule_id, scheduled_departure_at)
```

Trường hợp:

```text
Job 1 chạy
→ tạo OB-001

Job 2 chạy lại
→ thấy đã tồn tại
→ không tạo OB-002 cùng departure
```

---

# 14. Scheduler

Không cần Admin bấm nút.

Spring Scheduler có thể chạy hằng ngày:

```java
@Scheduled(cron = "0 5 0 * * *")
public void generateTrips() {
    tripGenerator.generateNextDays(7);
}
```

Flow:

```text
00:05 mỗi ngày
      ↓
Load ACTIVE schedules
      ↓
Generate next 7 days
      ↓
Insert missing TransportTrip
```

Sau này nếu scale lớn hơn có thể chuyển sang job/queue riêng, nhưng chưa cần ở Task 1.

---

# 15. Vehicle assignment

Task 1 giữ đơn giản.

Có thể cho `LinehaulSchedule` tham chiếu một Vehicle mặc định:

```text
S01
HN01 → HCM01
22:00
vehicle = V01
```

Khi Generator tạo:

```text
OB-001
vehicle = V01
```

Sau này mới phát triển:

```text
Vehicle Assignment
→ chọn vehicle khả dụng
→ đủ capacity
→ không bị conflict
```

Không biến Task 1 thành bài toán fleet scheduling.

---

# 16. Flyway implementation

## `karno-hub`

```text
V1__create_hub.sql
V2__create_hub_connection.sql
V3__seed_hub_network.sql
```

## `karno-fleet`

```text
V1__create_vehicle.sql
V2__create_linehaul_schedule.sql
V3__create_transport_trip.sql
V4__seed_vehicle_and_schedule.sql
```

Sau khi application chạy:

```text
Scheduler
    ↓
TripGeneratorService
    ↓
TransportTrip
```

---

# 17. Definition of Done

Task 1 hoàn thành khi:

### Hub network

```text
HN01
 ├── HCM01
 └── DN01

HCM01
 ├── CT01
 └── VL01

VL01
 └── CT01

DN01
 └── CT01
```

### Schedules

```text
HN01 → HCM01 | 22:00 DAILY | 8h
HCM01 → CT01 | 08:00 DAILY | 4h
HCM01 → VL01 | 06:00 DAILY | 2h
VL01 → CT01  | 09:30 DAILY | 2h
```

### Generated trips

Ví dụ:

```text
OB-001 | HN01 → HCM01 | 21/09 22:00 → 22/09 06:00
OB-002 | HN01 → HCM01 | 22/09 22:00 → 23/09 06:00
...
```

Không có duplicate.

---

# 18. Task 1 output

Sau Task 1, hệ thống có:

```text
Hub
    ↓
HubConnection
    ↓
LinehaulSchedule
    ↓
Trip Generator
    ↓
TransportTrip
```

Và Task 2 mới bắt đầu:

```text
Parcel
currentHub
destinationHub
readyAt
weight
        ↓
Candidate Trip Filtering
        ↓
Time-dependent Dijkstra
        ↓
RoutePlan
```

## Task name đề xuất

**HUB-001 — Network & Linehaul Foundation**

Subtasks:

```text
HUB-001.1  Design Hub & HubConnection schema
HUB-001.2  Design Vehicle schema
HUB-001.3  Design LinehaulSchedule schema
HUB-001.4  Design TransportTrip schema
HUB-001.5  Flyway migrations
HUB-001.6  Seed network / vehicle / schedule
HUB-001.7  Implement TripGeneratorService
HUB-001.8  Implement scheduled Trip generation
HUB-001.9  Add idempotency / unique constraint
HUB-001.10 Test generated TransportTrip data
```
