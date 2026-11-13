const pool = require('../config/db');

function toTaskJson(row) {
  return {
    id: String(row.task_id),
    label: row.label || `Phòng ${row.room_number}`,
    roomNumber: row.room_number,
    tier: row.room_tier,
    priority: row.priority,
    isDone: !!row.is_done,
    assignedTo: row.assigned_to || null,
    taskDate: row.task_date,
  };
}

// GET /api/housekeeping-tasks?date=2026-07-08&taskType=CLEAN&assignedTo=housekeeping1
// (tương đương getTasksByType(): lấy task hôm nay HOẶC task cũ chưa xong)
exports.getAll = async (req, res) => {
  const { date, taskType, assignedTo } = req.query;
  if (!date || !taskType) return res.status(400).json({ message: 'Thiếu tham số date hoặc taskType' });

  let sql = `
    SELECT t.*, r.room_tier
    FROM housekeeping_tasks t
    JOIN rooms r ON r.room_number = t.room_number
    WHERE ((t.task_date = ?) OR (t.task_date < ? AND t.is_done = 0)) AND t.task_type = ?
  `;
  const params = [date, date, taskType];
  if (assignedTo) { sql += ' AND t.assigned_to = ?'; params.push(assignedTo); }
  sql += ' ORDER BY t.task_id ASC';

  const [rows] = await pool.query(sql, params);
  res.json(rows.map(toTaskJson));
};

// GET /api/housekeeping-tasks/summary?date=2026-07-08&assignedTo=housekeeping1
// (tương đương getTaskSummary(): trả (total, done))
exports.summary = async (req, res) => {
  const { date, assignedTo } = req.query;
  if (!date) return res.status(400).json({ message: 'Thiếu tham số date' });

  let sql = `SELECT COUNT(*) AS total, SUM(is_done) AS done FROM housekeeping_tasks
             WHERE ((task_date = ?) OR (task_date < ? AND is_done = 0))`;
  const params = [date, date];
  if (assignedTo) { sql += ' AND assigned_to = ?'; params.push(assignedTo); }

  const [rows] = await pool.query(sql, params);
  res.json({ total: rows[0].total || 0, done: rows[0].done || 0 });
};

// POST /api/housekeeping-tasks  (Admin/Lễ tân giao việc thủ công)
exports.create = async (req, res) => {
  const { roomNumber, taskType, label, priority, assignedTo, taskDate } = req.body;
  if (!roomNumber || !taskType || !taskDate) return res.status(400).json({ message: 'Thiếu thông tin bắt buộc' });

  const [result] = await pool.query(
    `INSERT INTO housekeeping_tasks (room_number, task_type, label, priority, is_done, assigned_to, task_date)
     VALUES (?, ?, ?, ?, 0, ?, ?)`,
    [roomNumber, taskType, label || null, priority || 'NORMAL', assignedTo || null, taskDate]
  );
  res.status(201).json({ id: result.insertId, message: 'Tạo nhiệm vụ thành công' });
};

// PATCH /api/housekeeping-tasks/:id/assign
exports.assign = async (req, res) => {
  const { assignedTo } = req.body;
  await pool.query('UPDATE housekeeping_tasks SET assigned_to=? WHERE task_id=?', [assignedTo, req.params.id]);
  res.json({ message: 'Giao việc thành công' });
};

// PATCH /api/housekeeping-tasks/:id/done  (tương đương setTaskDone())
// Khi isDone=true: priority -> DONE, và nếu phòng không còn báo cáo sự cố mở -> phòng tự "available"
exports.setDone = async (req, res) => {
  const { isDone } = req.body;
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const [tasks] = await conn.query('SELECT * FROM housekeeping_tasks WHERE task_id=? FOR UPDATE', [req.params.id]);
    if (tasks.length === 0) throw { status: 404, message: 'Không tìm thấy nhiệm vụ' };
    const task = tasks[0];

    if (isDone) {
      await conn.query("UPDATE housekeeping_tasks SET is_done=1, priority='DONE' WHERE task_id=?", [req.params.id]);
    } else {
      await conn.query('UPDATE housekeeping_tasks SET is_done=0 WHERE task_id=?', [req.params.id]);
    }

    if (isDone) {
      const [openIssues] = await conn.query(
        "SELECT COUNT(*) AS cnt FROM maintenance_reports WHERE room_number=? AND status != 'DONE'",
        [task.room_number]
      );
      if (openIssues[0].cnt === 0) {
        await conn.query("UPDATE rooms SET status='available', updated_at=CURDATE() WHERE room_number=?", [task.room_number]);
      }
    }

    await conn.commit();
    res.json({ message: 'Cập nhật nhiệm vụ thành công' });
  } catch (err) {
    await conn.rollback();
    res.status(err.status || 500).json({ message: err.message || 'Lỗi server' });
  } finally {
    conn.release();
  }
};

// POST /api/housekeeping-tasks/periodic-check
// (tương đương checkPeriodicCleaning(): phòng "available" quá 7 ngày chưa đổi -> chuyển "maintenance" + tạo task)
// Gợi ý: gọi endpoint này bằng cron job (vd node-cron) chạy 1 lần/ngày, thay vì client tự gọi.
exports.periodicCheck = async (req, res) => {
  const conn = await pool.getConnection();
  try {
    const [rooms] = await conn.query(
      "SELECT room_number FROM rooms WHERE status='available' AND updated_at <= DATE_SUB(CURDATE(), INTERVAL 7 DAY)"
    );
    if (rooms.length === 0) return res.json({ message: 'Không có phòng nào cần dọn định kỳ', affected: 0 });

    await conn.beginTransaction();
    for (const r of rooms) {
      await conn.query("UPDATE rooms SET status='maintenance', updated_at=CURDATE() WHERE room_number=?", [r.room_number]);
      await conn.query(
        `INSERT INTO housekeeping_tasks (room_number, task_type, label, priority, is_done, task_date)
         VALUES (?, 'CLEAN', 'Vệ sinh định kỳ (Phòng trống > 7 ngày)', 'NORMAL', 0, CURDATE())`,
        [r.room_number]
      );
    }
    await conn.commit();
    res.json({ message: 'Đã tạo nhiệm vụ dọn dẹp định kỳ', affected: rooms.length });
  } catch (err) {
    await conn.rollback();
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  } finally {
    conn.release();
  }
};
