const pool = require('../config/db');

function toReportJson(row) {
  return {
    reportId: row.report_id,
    roomNumber: row.room_number,
    description: row.description,
    reportedBy: row.reported_by,
    status: row.status,
    createdAt: row.created_at,
    imagePath: row.image_path,
  };
}

// GET /api/maintenance-reports?status=PENDING  (tương đương getMaintenanceReports())
exports.getAll = async (req, res) => {
  const { status } = req.query;
  let sql = 'SELECT * FROM maintenance_reports';
  const params = [];
  if (status) { sql += ' WHERE status = ?'; params.push(status); }
  sql += ' ORDER BY report_id DESC';
  const [rows] = await pool.query(sql, params);
  res.json(rows.map(toReportJson));
};

// POST /api/maintenance-reports  (multipart/form-data, field ảnh: "image")
// (tương đương addMaintenanceReport(): phòng luôn chuyển sang "maintenance")
exports.create = async (req, res) => {
  const { roomNumber, description } = req.body;
  if (!roomNumber || !description) return res.status(400).json({ message: 'Thiếu thông tin' });

  const imagePath = req.file ? `/uploads/${req.file.filename}` : null;
  const now = Date.now().toString();

  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const [result] = await conn.query(
      `INSERT INTO maintenance_reports (room_number, description, reported_by, status, created_at, image_path)
       VALUES (?, ?, ?, 'PENDING', ?, ?)`,
      [roomNumber, description, req.user.username, now, imagePath]
    );
    await conn.query("UPDATE rooms SET status='maintenance', updated_at=CURDATE() WHERE room_number=?", [roomNumber]);
    await conn.commit();
    res.status(201).json({ reportId: result.insertId, imagePath, message: 'Gửi báo cáo thành công' });
  } catch (err) {
    await conn.rollback();
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  } finally {
    conn.release();
  }
};

// PATCH /api/maintenance-reports/:id/status  (tương đương updateMaintenanceReportStatus())
// Khi DONE -> phòng tự chuyển "available" (giữ đúng hành vi hiện tại của app, không kiểm tra housekeeping task)
exports.setStatus = async (req, res) => {
  const { status } = req.body; // PENDING | IN_PROGRESS | DONE
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const [reports] = await conn.query('SELECT * FROM maintenance_reports WHERE report_id=? FOR UPDATE', [req.params.id]);
    if (reports.length === 0) throw { status: 404, message: 'Không tìm thấy báo cáo' };
    const report = reports[0];

    await conn.query('UPDATE maintenance_reports SET status=? WHERE report_id=?', [status, req.params.id]);

    if (status === 'DONE') {
      await conn.query("UPDATE rooms SET status='available', updated_at=CURDATE() WHERE room_number=?", [report.room_number]);
    }

    await conn.commit();
    res.json({ message: 'Cập nhật trạng thái báo cáo thành công' });
  } catch (err) {
    await conn.rollback();
    res.status(err.status || 500).json({ message: err.message || 'Lỗi server' });
  } finally {
    conn.release();
  }
};
