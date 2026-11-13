const pool = require('../config/db');

function toCustomerJson(row) {
  return {
    customerId: row.customer_id,
    fullName: row.full_name,
    phone: row.phone || '',
    idCard: row.id_card || '',
    email: row.email || '',
    address: row.address || '',
  };
}

// GET /api/customers?phone=0987xxxxxx  (tương đương findCustomerByPhone khi có phone, ngược lại trả tất cả)
exports.getAll = async (req, res) => {
  const { phone } = req.query;
  let sql = 'SELECT * FROM customers';
  const params = [];
  if (phone) { sql += ' WHERE phone = ?'; params.push(phone); }
  sql += ' ORDER BY customer_id DESC';
  const [rows] = await pool.query(sql, params);
  res.json(rows.map(toCustomerJson));
};

// GET /api/customers/status  (tương đương getAllCustomersWithStatus(): STAYING / ARRIVING / LEFT)
exports.withStatus = async (req, res) => {
  const [staying] = await pool.query(`
    SELECT c.customer_id, c.full_name, b.room_number, r.room_type, c.phone
    FROM bookings b JOIN customers c ON c.customer_id=b.customer_id JOIN rooms r ON r.room_number=b.room_number
    WHERE b.status='CHECKED_IN'`);
  const [arriving] = await pool.query(`
    SELECT c.customer_id, c.full_name, b.room_number, r.room_type, c.phone
    FROM bookings b JOIN customers c ON c.customer_id=b.customer_id JOIN rooms r ON r.room_number=b.room_number
    WHERE b.status='BOOKED'`);
  const [left] = await pool.query(`
    SELECT c.customer_id, c.full_name, b.room_number, r.room_type, c.phone
    FROM bookings b JOIN customers c ON c.customer_id=b.customer_id JOIN rooms r ON r.room_number=b.room_number
    WHERE b.status='CHECKED_OUT' AND b.booking_id = (SELECT MAX(b2.booking_id) FROM bookings b2 WHERE b2.customer_id=c.customer_id)`);

  const map = (rows, status) => rows.map(r => ({
    id: String(r.customer_id), name: r.full_name, roomNumber: r.room_number,
    roomType: r.room_type, status, phone: r.phone || '',
  }));

  res.json([...map(staying, 'STAYING'), ...map(arriving, 'ARRIVING'), ...map(left, 'LEFT')]);
};

// GET /api/customers/:id
exports.getById = async (req, res) => {
  const [rows] = await pool.query('SELECT * FROM customers WHERE customer_id = ?', [req.params.id]);
  if (rows.length === 0) return res.status(404).json({ message: 'Không tìm thấy khách hàng' });
  res.json(toCustomerJson(rows[0]));
};

// POST /api/customers  (tương đương addCustomer())
exports.create = async (req, res) => {
  const { fullName, phone, idCard, email, address } = req.body;
  if (!fullName) return res.status(400).json({ message: 'Thiếu tên khách hàng' });
  const [result] = await pool.query(
    'INSERT INTO customers (full_name, phone, id_card, email, address) VALUES (?, ?, ?, ?, ?)',
    [fullName, phone || null, idCard || null, email || null, address || null]
  );
  res.status(201).json({ customerId: result.insertId, message: 'Tạo khách hàng thành công' });
};

// PUT /api/customers/:id
exports.update = async (req, res) => {
  const { fullName, phone, idCard, email, address } = req.body;
  await pool.query(
    'UPDATE customers SET full_name=?, phone=?, id_card=?, email=?, address=? WHERE customer_id=?',
    [fullName, phone, idCard, email, address, req.params.id]
  );
  res.json({ message: 'Cập nhật khách hàng thành công' });
};
