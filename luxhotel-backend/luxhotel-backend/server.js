require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');

const authRoutes = require('./src/routes/authRoutes');
const userRoutes = require('./src/routes/userRoutes');
const roomRoutes = require('./src/routes/roomRoutes');
const customerRoutes = require('./src/routes/customerRoutes');
const bookingRoutes = require('./src/routes/bookingRoutes');
const housekeepingRoutes = require('./src/routes/housekeepingRoutes');
const maintenanceRoutes = require('./src/routes/maintenanceRoutes');
const reportRoutes = require('./src/routes/reportRoutes');

const app = express();

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Cho phép truy cập ảnh đã upload (báo cáo sự cố, avatar) qua URL: http://<host>:<port>/uploads/<file>
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

app.get('/health', (req, res) => res.json({ status: 'ok', time: new Date().toISOString() }));

app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/rooms', roomRoutes);
app.use('/api/customers', customerRoutes);
app.use('/api/bookings', bookingRoutes);
app.use('/api/housekeeping-tasks', housekeepingRoutes);
app.use('/api/maintenance-reports', maintenanceRoutes);
app.use('/api/reports', reportRoutes);

// 404 handler
app.use((req, res) => res.status(404).json({ message: 'Endpoint không tồn tại' }));

// Error handler chung
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ message: 'Lỗi server không xác định', error: err.message });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`✅ LuxHotel backend đang chạy tại http://localhost:${PORT}`);
});
