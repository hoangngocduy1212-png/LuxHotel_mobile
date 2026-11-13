const pool = require('../config/db');
const { hashPassword } = require('../utils/password');

const SAFE_COLS = 'username, role, display_name, phone, email, department, join_date, staff_id, is_active, avatar_path';

function toUserJson(row) {
  return {
    username: row.username,
    role: row.role,
    displayName: row.display_name,
    isActive: !!row.is_active,
    phone: row.phone || '',
    email: row.email || '',
    department: row.department || '',
    joinDate: row.join_date || '',
    staffId: row.staff_id || '',
    avatarPath: row.avatar_path,
  };
}

// GET /api/users?role=RECEPTIONIST   (tương đương getAllUsers(role))
exports.getAll = async (req, res) => {
  const { role } = req.query;
  let sql = `SELECT ${SAFE_COLS} FROM users`;
  const params = [];
  if (role) { sql += ' WHERE role = ?'; params.push(role); }
  const [rows] = await pool.query(sql, params);
  res.json(rows.map(toUserJson));
};

// GET /api/users/:username  (tương đương getUser(username))
exports.getById = async (req, res) => {
  const [rows] = await pool.query(`SELECT ${SAFE_COLS} FROM users WHERE username = ?`, [req.params.username]);
  if (rows.length === 0) return res.status(404).json({ message: 'Không tìm thấy nhân viên' });
  res.json(toUserJson(rows[0]));
};

// POST /api/users  (tương đương addUser)
exports.create = async (req, res) => {
  try {
    const { username, password, role, displayName, phone, email, department, joinDate, staffId } = req.body;
    if (!username || !password || !role || !displayName) {
      return res.status(400).json({ message: 'Thiếu thông tin bắt buộc' });
    }
    const hashed = hashPassword(password);
    await pool.query(
      `INSERT INTO users (username, password, role, display_name, phone, email, department, join_date, staff_id, is_active)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)`,
      [username, hashed, role, displayName, phone || null, email || null, department || null, joinDate || null, staffId || null]
    );
    res.status(201).json({ username, message: 'Tạo nhân viên thành công' });
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') return res.status(409).json({ message: 'Username đã tồn tại' });
    console.error(err);
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
};

// PUT /api/users/:username  (tương đương updateUserProfile)
exports.update = async (req, res) => {
  const { displayName, phone } = req.body;
  await pool.query('UPDATE users SET display_name=?, phone=? WHERE username=?', [displayName, phone, req.params.username]);
  res.json({ message: 'Cập nhật thành công' });
};

// PATCH /api/users/:username/password  (tương đương changePassword)
exports.changePassword = async (req, res) => {
  const { newPassword } = req.body;
  if (!newPassword) return res.status(400).json({ message: 'Thiếu mật khẩu mới' });
  await pool.query('UPDATE users SET password=? WHERE username=?', [hashPassword(newPassword), req.params.username]);
  res.json({ message: 'Đổi mật khẩu thành công' });
};

// PATCH /api/users/:username/status  (tương đương setUserActive)
exports.setStatus = async (req, res) => {
  const { isActive } = req.body;
  await pool.query('UPDATE users SET is_active=? WHERE username=?', [isActive ? 1 : 0, req.params.username]);
  res.json({ message: 'Cập nhật trạng thái thành công' });
};

// PATCH /api/users/:username/avatar  (tương đương updateUserAvatar)
exports.updateAvatar = async (req, res) => {
  const avatarPath = req.file ? `/uploads/${req.file.filename}` : (req.body.avatarPath || null);
  await pool.query('UPDATE users SET avatar_path=? WHERE username=?', [avatarPath, req.params.username]);
  res.json({ avatarPath, message: 'Cập nhật ảnh đại diện thành công' });
};
