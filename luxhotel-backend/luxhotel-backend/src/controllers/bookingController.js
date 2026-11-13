const pool = require('../config/db');

function calculateNights(checkin, checkout) {
  const d1 = new Date(checkin).getTime();
  const d2 = new Date(checkout).getTime();
  const diff = Math.round((d2 - d1) / (1000 * 60 * 60 * 24));
  return diff < 1 ? 1 : diff;
}

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

function mapCheckoutStatus(bookingStatus, checkoutDate) {
  if (bookingStatus === 'CHECKED_OUT') return 'CHECKED_OUT';
  const today = todayStr();
  return checkoutDate < today ? 'OVERDUE' : 'DUE_TODAY';
}

async function getServiceCharges(bookingId) {
  const [rows] = await pool.query('SELECT name, amount FROM service_charges WHERE booking_id = ?', [bookingId]);
  return rows.map(r => ({ name: r.name, amount: Number(r.amount) }));
}

// GET /api/bookings?status=BOOKED  (tương đương getBookingsByStatus() / getPendingCheckIns())
exports.getByStatus = async (req, res) => {
  const { status } = req.query;
  if (!status) return res.status(400).json({ message: 'Thiếu tham số status' });

  const [rows] = await pool.query(`
    SELECT b.booking_id, b.room_number, r.room_type, c.full_name AS guest_name, c.phone AS guest_phone,
           c.id_card AS guest_id_card, b.checkin_date, b.checkout_date, b.price_per_night, b.status
    FROM bookings b
    JOIN rooms r ON r.room_number = b.room_number
    JOIN customers c ON c.customer_id = b.customer_id
    WHERE b.status = ?
    ORDER BY b.booking_id DESC
  `, [status]);

  const result = [];
  for (const row of rows) {
    result.push({
      bookingId: String(row.booking_id),
      roomNumber: row.room_number,
      roomType: row.room_type,
      guestName: row.guest_name,
      guestPhone: row.guest_phone || '',
      guestIdCard: row.guest_id_card || '',
      checkInDate: row.checkin_date,
      checkOutDate: row.checkout_date,
      nights: calculateNights(row.checkin_date, row.checkout_date),
      roomPricePerNight: Number(row.price_per_night),
      serviceCharges: await getServiceCharges(row.booking_id),
      status: mapCheckoutStatus(row.status, row.checkout_date),
    });
  }
  res.json(result);
};

// GET /api/bookings/:id
exports.getById = async (req, res) => {
  const [rows] = await pool.query(`
    SELECT b.*, r.room_type, c.full_name AS guest_name, c.phone AS guest_phone, c.id_card AS guest_id_card
    FROM bookings b
    JOIN rooms r ON r.room_number = b.room_number
    JOIN customers c ON c.customer_id = b.customer_id
    WHERE b.booking_id = ?
  `, [req.params.id]);
  if (rows.length === 0) return res.status(404).json({ message: 'Không tìm thấy booking' });
  const row = rows[0];
  res.json({
    bookingId: String(row.booking_id),
    roomNumber: row.room_number,
    roomType: row.room_type,
    guestName: row.guest_name,
    guestPhone: row.guest_phone || '',
    guestIdCard: row.guest_id_card || '',
    checkInDate: row.checkin_date,
    checkOutDate: row.checkout_date,
    actualCheckin: row.actual_checkin,
    actualCheckout: row.actual_checkout,
    status: row.status,
    roomPricePerNight: Number(row.price_per_night),
    serviceCharges: await getServiceCharges(row.booking_id),
  });
};

// POST /api/bookings  (tương đương createBooking())
// body: { roomNumber, customerId, checkinDate, checkoutDate, pricePerNight, checkInNow }
exports.create = async (req, res) => {
  const { roomNumber, customerId, checkinDate, checkoutDate, pricePerNight, checkInNow } = req.body;
  if (!roomNumber || !customerId || !checkinDate || !checkoutDate || pricePerNight == null) {
    return res.status(400).json({ message: 'Thiếu thông tin bắt buộc' });
  }

  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();

    const [rooms] = await conn.query('SELECT * FROM rooms WHERE room_number = ? FOR UPDATE', [roomNumber]);
    if (rooms.length === 0) throw { status: 404, message: 'Phòng không tồn tại' };
    if (rooms[0].status !== 'available') throw { status: 409, message: 'Phòng hiện không sẵn sàng để đặt' };

    const now = Date.now().toString();
    const status = checkInNow ? 'CHECKED_IN' : 'BOOKED';
    const actualCheckin = checkInNow ? now : null;

    const [result] = await conn.query(
      `INSERT INTO bookings (room_number, customer_id, checkin_date, checkout_date, actual_checkin, status, price_per_night, created_by, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [roomNumber, customerId, checkinDate, checkoutDate, actualCheckin, status, pricePerNight, req.user.username, now]
    );

    const newRoomStatus = checkInNow ? 'occupied' : 'booked';
    await conn.query('UPDATE rooms SET status=?, updated_at=CURDATE() WHERE room_number=?', [newRoomStatus, roomNumber]);

    await conn.commit();
    res.status(201).json({ bookingId: result.insertId, message: 'Đặt phòng thành công' });
  } catch (err) {
    await conn.rollback();
    res.status(err.status || 500).json({ message: err.message || 'Lỗi server' });
  } finally {
    conn.release();
  }
};

// POST /api/bookings/:id/checkin  (tương đương checkIn())
exports.checkin = async (req, res) => {
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const [bookings] = await conn.query('SELECT * FROM bookings WHERE booking_id = ? FOR UPDATE', [req.params.id]);
    if (bookings.length === 0) throw { status: 404, message: 'Không tìm thấy booking' };
    const booking = bookings[0];
    if (booking.status !== 'BOOKED') throw { status: 409, message: 'Booking không ở trạng thái BOOKED' };

    const now = Date.now().toString();
    await conn.query("UPDATE bookings SET status='CHECKED_IN', actual_checkin=? WHERE booking_id=?", [now, req.params.id]);
    await conn.query("UPDATE rooms SET status='occupied', updated_at=CURDATE() WHERE room_number=?", [booking.room_number]);

    await conn.commit();
    res.json({ message: 'Check-in thành công' });
  } catch (err) {
    await conn.rollback();
    res.status(err.status || 500).json({ message: err.message || 'Lỗi server' });
  } finally {
    conn.release();
  }
};

// POST /api/bookings/:id/checkout  (tương đương checkOut(): tự tạo housekeeping task)
exports.checkout = async (req, res) => {
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const [bookings] = await conn.query('SELECT * FROM bookings WHERE booking_id = ? FOR UPDATE', [req.params.id]);
    if (bookings.length === 0) throw { status: 404, message: 'Không tìm thấy booking' };
    const booking = bookings[0];
    if (booking.status !== 'CHECKED_IN') throw { status: 409, message: 'Booking chưa check-in, không thể check-out' };

    const now = Date.now().toString();
    await conn.query("UPDATE bookings SET status='CHECKED_OUT', actual_checkout=? WHERE booking_id=?", [now, req.params.id]);
    await conn.query("UPDATE rooms SET status='maintenance', updated_at=CURDATE() WHERE room_number=?", [booking.room_number]);

    await conn.query(
      `INSERT INTO housekeeping_tasks (room_number, task_type, label, priority, is_done, task_date)
       VALUES (?, 'CLEAN', ?, 'NORMAL', 0, CURDATE())`,
      [booking.room_number, `Dọn phòng ${booking.room_number} sau khi khách trả phòng`]
    );

    // Tính tổng tiền để hiển thị hóa đơn (không lưu cột total, tính động giống app)
    const nights = calculateNights(booking.checkin_date, booking.checkout_date);
    const roomTotal = nights * Number(booking.price_per_night);
    const charges = await getServiceCharges(req.params.id);
    const serviceTotal = charges.reduce((s, c) => s + c.amount, 0);

    await conn.commit();
    res.json({
      message: 'Check-out thành công',
      nights,
      roomTotal,
      serviceTotal,
      totalAmount: roomTotal + serviceTotal,
    });
  } catch (err) {
    await conn.rollback();
    res.status(err.status || 500).json({ message: err.message || 'Lỗi server' });
  } finally {
    conn.release();
  }
};

// POST /api/bookings/:id/service-charges  (thêm phí dịch vụ phát sinh)
exports.addServiceCharge = async (req, res) => {
  const { name, amount } = req.body;
  if (!name || amount == null) return res.status(400).json({ message: 'Thiếu thông tin' });
  const [result] = await pool.query(
    'INSERT INTO service_charges (booking_id, name, amount) VALUES (?, ?, ?)',
    [req.params.id, name, amount]
  );
  res.status(201).json({ serviceId: result.insertId, message: 'Thêm phí dịch vụ thành công' });
};

// GET /api/reports/revenue?year=2026&month=7  (tương đương getRevenueByMonth())
exports.revenueByMonth = async (req, res) => {
  const now = new Date();
  const year = Number(req.query.year) || now.getFullYear();
  const month = Number(req.query.month) || now.getMonth() + 1;
  const monthStr = `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}`;

  const [bookings] = await pool.query(
    `SELECT booking_id, price_per_night, checkin_date, checkout_date
     FROM bookings WHERE checkout_date LIKE ? AND status = 'CHECKED_OUT'`,
    [`${monthStr}%`]
  );

  let total = 0;
  for (const b of bookings) {
    const nights = calculateNights(b.checkin_date, b.checkout_date);
    total += nights * Number(b.price_per_night);
    const [sc] = await pool.query('SELECT COALESCE(SUM(amount),0) AS sum FROM service_charges WHERE booking_id=?', [b.booking_id]);
    total += Number(sc[0].sum);
  }

  res.json({ year, month, totalRevenue: total, totalBookings: bookings.length });
};
