const pool = require('../config/db');

function toRoomInfoJson(row) {
  return {
    roomNumber: row.room_number,
    roomType: row.room_type,
    status: row.status,
    price: Number(row.price_per_night),
    bedCount: row.bed_count != null ? Number(row.bed_count) : 1,
    guestName: row.guest_name || null,
  };
}

// GET /api/rooms  (tương đương getAllRoomsInfo(): kèm tên khách đang ở nếu có)
exports.getAll = async (req, res) => {
  const [rows] = await pool.query(`
    SELECT r.room_number, r.room_type, r.status, r.price_per_night, d.bed_count, c.full_name AS guest_name
    FROM rooms r
    LEFT JOIN room_details d ON r.room_number = d.room_number
    LEFT JOIN bookings b ON b.room_number = r.room_number AND b.status = 'CHECKED_IN'
    LEFT JOIN customers c ON c.customer_id = b.customer_id
    ORDER BY CAST(r.room_number AS UNSIGNED) ASC
  `);
  res.json(rows.map(toRoomInfoJson));
};

// GET /api/rooms/available  (tương đương getAvailableRooms())
exports.getAvailable = async (req, res) => {
  const [rows] = await pool.query(`
    SELECT r.room_number, r.room_type, r.status, r.price_per_night, d.bed_count, NULL AS guest_name
    FROM rooms r
    LEFT JOIN room_details d ON r.room_number = d.room_number
    WHERE r.status = 'available'
    ORDER BY CAST(r.room_number AS UNSIGNED) ASC
  `);
  res.json(rows.map(toRoomInfoJson));
};

// GET /api/rooms/status-counts  (tương đương getRoomStatusCounts())
exports.statusCounts = async (req, res) => {
  const [rows] = await pool.query('SELECT status, COUNT(*) AS count FROM rooms GROUP BY status');
  const result = { available: 0, occupied: 0, maintenance: 0, booked: 0 };
  rows.forEach(r => { result[r.status] = r.count; });
  res.json(result);
};

// GET /api/rooms/calendar?date=2026-07-08
// (tương đương getRoomsForDate + getRoomCountsForDate cho RoomCalendarActivity)
exports.calendar = async (req, res) => {
  const { date } = req.query;
  if (!date) return res.status(400).json({ message: 'Thiếu tham số date (yyyy-MM-dd)' });

  const [rows] = await pool.query(`
    SELECT r.room_number, r.room_type, r.status, r.price_per_night, d.bed_count, c.full_name AS guest_name
    FROM rooms r
    LEFT JOIN room_details d ON r.room_number = d.room_number
    LEFT JOIN bookings b ON b.room_number = r.room_number AND b.status != 'CANCELLED'
      AND b.checkin_date <= ? AND b.checkout_date > ?
    LEFT JOIN customers c ON c.customer_id = b.customer_id
    ORDER BY CAST(r.room_number AS UNSIGNED) ASC
  `, [date, date]);

  const rooms = rows.map(r => ({
    ...toRoomInfoJson(r),
    status: r.guest_name ? 'occupied' : r.status,
  }));

  const occupied = rooms.filter(r => r.guestName).length;
  const [totalRes] = await pool.query('SELECT COUNT(*) AS cnt FROM rooms');
  const total = totalRes[0].cnt;
  const [maintRes] = await pool.query("SELECT COUNT(*) AS cnt FROM rooms WHERE status='maintenance'");
  const maintenance = maintRes[0].cnt;
  const available = Math.max(0, total - occupied - maintenance);

  res.json({ date, available, occupied, maintenance, rooms });
};

// GET /api/rooms/:room_number  (chi tiết 1 phòng + room_details)
exports.getById = async (req, res) => {
  const [rooms] = await pool.query('SELECT * FROM rooms WHERE room_number = ?', [req.params.room_number]);
  if (rooms.length === 0) return res.status(404).json({ message: 'Không tìm thấy phòng' });
  const [details] = await pool.query('SELECT * FROM room_details WHERE room_number = ?', [req.params.room_number]);
  res.json({ ...toRoomInfoJson({ ...rooms[0], bed_count: details[0]?.bed_count }), amenities: details[0]?.amenities || '', description: rooms[0].description });
};

// POST /api/rooms  (tương đương addRoom(), Admin thêm phòng mới)
exports.create = async (req, res) => {
  const { roomNumber, roomType, tier, price, floor, bedCount, description } = req.body;
  if (!roomNumber || !roomType || price == null) return res.status(400).json({ message: 'Thiếu thông tin bắt buộc' });

  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    await conn.query(
      `INSERT INTO rooms (room_number, room_type, room_tier, price_per_night, status, floor, description, updated_at)
       VALUES (?, ?, ?, ?, 'available', ?, ?, CURDATE())`,
      [roomNumber, roomType, tier || 'STANDARD', price, floor || 1, description || null]
    );
    await conn.query(
      'INSERT INTO room_details (room_number, bed_count) VALUES (?, ?)',
      [roomNumber, bedCount || 1]
    );
    await conn.commit();
    res.status(201).json({ roomNumber, message: 'Tạo phòng thành công' });
  } catch (err) {
    await conn.rollback();
    if (err.code === 'ER_DUP_ENTRY') return res.status(409).json({ message: 'Số phòng đã tồn tại' });
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  } finally {
    conn.release();
  }
};

// PATCH /api/rooms/:room_number/status  (tương đương updateRoomStatus())
exports.setStatus = async (req, res) => {
  const { status } = req.body; // available | occupied | maintenance | booked
  await pool.query('UPDATE rooms SET status=?, updated_at=CURDATE() WHERE room_number=?', [status, req.params.room_number]);
  res.json({ message: 'Cập nhật trạng thái phòng thành công' });
};
