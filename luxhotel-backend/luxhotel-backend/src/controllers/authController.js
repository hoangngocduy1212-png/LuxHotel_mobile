const pool = require('../config/db');
const { hashPassword } = require('../utils/password');
const { signToken } = require('../utils/jwt');

// POST /api/auth/login
// Tương đương DatabaseHelper.login(username, password)
exports.login = async (req, res) => {
  try {
    const { username, password } = req.body;
    if (!username || !password) {
      return res.status(400).json({ message: 'Thiếu username hoặc password' });
    }

    const hashed = hashPassword(password);
    const [rows] = await pool.query(
      'SELECT * FROM users WHERE username = ? AND password = ?',
      [username, hashed]
    );

    if (rows.length === 0) {
      return res.status(401).json({ message: 'Sai tài khoản hoặc mật khẩu' });
    }

    const user = rows[0];
    if (!user.is_active) {
      return res.status(403).json({ message: 'Tài khoản đã bị vô hiệu hóa' });
    }

    const token = signToken({ username: user.username, role: user.role });

    // Trả về đúng field name giống data class User trong Kotlin
    res.json({
      token,
      user: {
        username: user.username,
        role: user.role,
        displayName: user.display_name,
        isActive: !!user.is_active,
        phone: user.phone || '',
        email: user.email || '',
        department: user.department || '',
        joinDate: user.join_date || '',
        staffId: user.staff_id || '',
        avatarPath: user.avatar_path,
      },
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
};
