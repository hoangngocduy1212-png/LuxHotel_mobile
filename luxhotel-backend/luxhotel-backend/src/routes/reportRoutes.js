const router = require('express').Router();
const reportController = require('../controllers/reportController');
const bookingController = require('../controllers/bookingController');
const { authenticate, authorize } = require('../middleware/auth');

router.use(authenticate, authorize('ADMIN'));

router.get('/dashboard', reportController.dashboard);
router.get('/revenue', bookingController.revenueByMonth);

module.exports = router;
