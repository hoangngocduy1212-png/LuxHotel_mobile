CREATE DATABASE IF NOT EXISTS luxhotel CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE luxhotel;

-- ===================== USERS =====================
CREATE TABLE IF NOT EXISTS users (
  username      VARCHAR(50) PRIMARY KEY,
  password      VARCHAR(64) NOT NULL,        -- SHA-256 hex = 64 ký tự
  role          VARCHAR(20) NOT NULL,        -- ADMIN | RECEPTIONIST | HOUSEKEEPING
  display_name  VARCHAR(100) NOT NULL,
  phone         VARCHAR(20),
  email         VARCHAR(100),
  department    VARCHAR(100),
  join_date     VARCHAR(20),
  staff_id      VARCHAR(20),
  is_active     TINYINT(1) NOT NULL DEFAULT 1,
  avatar_path   VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================== ROOMS =====================
CREATE TABLE IF NOT EXISTS rooms (
  room_number      VARCHAR(20) PRIMARY KEY,
  room_type        VARCHAR(50) NOT NULL,
  room_tier        VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
  price_per_night  BIGINT NOT NULL DEFAULT 0,
  status           VARCHAR(20) NOT NULL DEFAULT 'available',
  floor            INT DEFAULT 1,
  description      TEXT,
  updated_at       VARCHAR(20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================== ROOM DETAILS =====================
CREATE TABLE IF NOT EXISTS room_details (
  room_number  VARCHAR(20) PRIMARY KEY,
  bed_count    INT NOT NULL DEFAULT 1,
  amenities    TEXT,
  FOREIGN KEY (room_number) REFERENCES rooms(room_number) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================== CUSTOMERS =====================
CREATE TABLE IF NOT EXISTS customers (
  customer_id  INT AUTO_INCREMENT PRIMARY KEY,
  full_name    VARCHAR(100) NOT NULL,
  phone        VARCHAR(20),
  id_card      VARCHAR(20),
  email        VARCHAR(100),
  address      VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================== BOOKINGS =====================
CREATE TABLE IF NOT EXISTS bookings (
  booking_id       INT AUTO_INCREMENT PRIMARY KEY,
  room_number      VARCHAR(20) NOT NULL,
  customer_id      INT NOT NULL,
  checkin_date     VARCHAR(20) NOT NULL,
  checkout_date    VARCHAR(20) NOT NULL,
  actual_checkin   VARCHAR(30),
  actual_checkout  VARCHAR(30),
  status           VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
  price_per_night  BIGINT NOT NULL DEFAULT 0,
  created_by       VARCHAR(50),
  created_at       VARCHAR(30),
  INDEX idx_bookings_room (room_number),
  INDEX idx_bookings_customer (customer_id),
  INDEX idx_bookings_created_by (created_by),
  FOREIGN KEY (room_number) REFERENCES rooms(room_number),
  FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
  FOREIGN KEY (created_by) REFERENCES users(username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================== SERVICE CHARGES =====================
CREATE TABLE IF NOT EXISTS service_charges (
  service_id  INT AUTO_INCREMENT PRIMARY KEY,
  booking_id  INT NOT NULL,
  name        VARCHAR(100) NOT NULL,
  amount      BIGINT NOT NULL DEFAULT 0,
  INDEX idx_service_booking (booking_id),
  FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================== HOUSEKEEPING TASKS =====================
CREATE TABLE IF NOT EXISTS housekeeping_tasks (
  task_id      INT AUTO_INCREMENT PRIMARY KEY,
  room_number  VARCHAR(20) NOT NULL,
  task_type    VARCHAR(20) NOT NULL,
  label        VARCHAR(255),
  priority     VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  is_done      TINYINT(1) NOT NULL DEFAULT 0,
  assigned_to  VARCHAR(50),
  task_date    VARCHAR(20) NOT NULL,
  INDEX idx_hk_room (room_number),
  INDEX idx_hk_assigned (assigned_to),
  FOREIGN KEY (room_number) REFERENCES rooms(room_number),
  FOREIGN KEY (assigned_to) REFERENCES users(username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================== MAINTENANCE REPORTS =====================
CREATE TABLE IF NOT EXISTS maintenance_reports (
  report_id    INT AUTO_INCREMENT PRIMARY KEY,
  room_number  VARCHAR(20) NOT NULL,
  description  TEXT NOT NULL,
  reported_by  VARCHAR(50),
  status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at   VARCHAR(30),
  image_path   VARCHAR(255),
  INDEX idx_maint_room (room_number),
  INDEX idx_maint_reported_by (reported_by),
  FOREIGN KEY (room_number) REFERENCES rooms(room_number),
  FOREIGN KEY (reported_by) REFERENCES users(username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================== DỮ LIỆU MẪU =====================

INSERT INTO users (username, password, role, display_name, phone, email, department, join_date, staff_id, is_active) VALUES
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN', 'Quản trị viên', '0900000000', 'admin@luxhotel.com', 'Management', '01/01/2024', 'ADM-0001', 1),
('receptionist1', 'c5983678539c3f4fadcbd93f59570758f18698a4807b78ed63708f12f199b465', 'RECEPTIONIST', 'Lễ tân Minh Anh', '0987654321', 'minhanh@luxhotel.com', 'Front Office', '15/03/2024', 'RC-0015', 1),
('receptionist2', 'c5983678539c3f4fadcbd93f59570758f18698a4807b78ed63708f12f199b465', 'RECEPTIONIST', 'Lễ tân Tuấn Kiệt', '0912345678', 'tuankiet@luxhotel.com', 'Front Office', '10/05/2024', 'RC-0016', 1),
('housekeeping1', '45e950369a37fd1d353b2af26831b06e7260795338b21dd7153f772c60ef46b9', 'HOUSEKEEPING', 'Buồng phòng Thu Thủy', '0933445566', 'thuthuy@luxhotel.com', 'Housekeeping', '20/02/2024', 'HK-0042', 1),
('housekeeping2', '45e950369a37fd1d353b2af26831b06e7260795338b21dd7153f772c60ef46b9', 'HOUSEKEEPING', 'Buồng phòng Văn Nam', '0944556677', 'vannam@luxhotel.com', 'Housekeeping', '05/04/2024', 'HK-0043', 1),
('housekeeping3', '45e950369a37fd1d353b2af26831b06e7260795338b21dd7153f772c60ef46b9', 'HOUSEKEEPING', 'Buồng phòng Hồng Hạnh', '0955667788', 'honghanh@luxhotel.com', 'Housekeeping', '12/06/2024', 'HK-0044', 1)
ON DUPLICATE KEY UPDATE username = username;

INSERT INTO rooms (room_number, room_type, room_tier, price_per_night, status, floor, description, updated_at) VALUES
('1', 'Standard', 'STANDARD', 500000, 'available', 1, 'Phòng tiêu chuẩn 1 giường đôi', CURDATE()),
('2', 'Standard', 'STANDARD', 500000, 'available', 1, 'Phòng tiêu chuẩn 2 giường đơn', CURDATE()),
('3', 'Standard', 'STANDARD', 500000, 'available', 1, 'Phòng tiêu chuẩn view sân vườn', CURDATE()),
('4', 'Deluxe', 'STANDARD', 800000, 'available', 2, 'Phòng deluxe 1 giường king', CURDATE()),
('5', 'Deluxe', 'STANDARD', 800000, 'available', 2, 'Phòng deluxe 2 giường đơn', CURDATE()),
('6', 'Deluxe', 'STANDARD', 800000, 'available', 2, 'Phòng deluxe view hồ bơi', CURDATE()),
('7', 'Superior', 'STANDARD', 1000000, 'available', 3, 'Phòng superior ban công riêng', CURDATE()),
('8', 'Superior', 'STANDARD', 1000000, 'available', 3, 'Phòng superior view thành phố', CURDATE()),
('9', 'Superior', 'STANDARD', 1000000, 'available', 3, 'Phòng superior giường king', CURDATE()),
('10', 'VIP Suite', 'VIP', 1500000, 'available', 4, 'Suite VIP phòng khách riêng', CURDATE()),
('11', 'VIP Suite', 'VIP', 1500000, 'available', 4, 'Suite VIP 2 phòng ngủ', CURDATE()),
('12', 'VIP Suite', 'VIP', 1500000, 'available', 4, 'Suite VIP view toàn cảnh', CURDATE()),
('13', 'Presidential', 'VIP', 2500000, 'available', 5, 'Phòng tổng thống, diện tích 80m²', CURDATE()),
('14', 'Presidential', 'VIP', 2500000, 'available', 5, 'Phòng tổng thống góc, view 2 chiều', CURDATE()),
('15', 'Penthouse', 'VIP', 3500000, 'available', 5, 'Penthouse tầng thượng, sân hiên riêng', CURDATE())
ON DUPLICATE KEY UPDATE room_number = room_number;

-- FIX: thêm tiền tố "r." cho toàn bộ cột lấy từ bảng rooms để tránh lỗi
-- "Column 'room_number' is ambiguous" (do room_details cũng có cột room_number)
INSERT INTO room_details (room_number, bed_count, amenities)
SELECT r.room_number,
  CASE r.room_number WHEN '2' THEN 2 WHEN '5' THEN 2 WHEN '11' THEN 2 WHEN '14' THEN 2 ELSE 1 END,
  'Wifi, Điều hòa, TV, Tủ lạnh'
FROM rooms r
ON DUPLICATE KEY UPDATE bed_count = VALUES(bed_count);
