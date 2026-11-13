const pool = require('../config/db');

// GET /api/reports/dashboard  (Tổng quan cho AdminDashboardActivity)
exports.dashboard = async (req, res) => {
  const [roomStats] = await pool.query('SELECT status, COUNT(*) AS count FROM rooms GROUP BY status');
  const today = new Date().toISOString().slice(0, 10);

  const [todayCheckins] = await pool.query(
    "SELECT COUNT(*) AS cnt FROM bookings WHERE checkin_date = ? AND status = 'BOOKED'", [today]
  );
  const [todayCheckouts] = await pool.query(
    "SELECT COUNT(*) AS cnt FROM bookings WHERE checkout_date <= ? AND status = 'CHECKED_IN'", [today]
  );
  const [pendingMaintenance] = await pool.query(
    "SELECT COUNT(*) AS cnt FROM maintenance_reports WHERE status != 'DONE'"
  );

  const roomStatusMap = { available: 0, occupied: 0, maintenance: 0, booked: 0 };
  roomStats.forEach(r => { roomStatusMap[r.status] = r.count; });

  res.json({
    roomStatus: roomStatusMap,
    todayCheckins: todayCheckins[0].cnt,
    todayCheckouts: todayCheckouts[0].cnt,
    pendingMaintenance: pendingMaintenance[0].cnt,
  });
};
