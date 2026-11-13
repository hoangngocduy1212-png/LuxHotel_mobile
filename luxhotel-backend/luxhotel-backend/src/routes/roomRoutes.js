const router = require('express').Router();
const roomController = require('../controllers/roomController');
const { authenticate, authorize } = require('../middleware/auth');

router.use(authenticate);

router.get('/', roomController.getAll);
router.get('/available', roomController.getAvailable);
router.get('/status-counts', roomController.statusCounts);
router.get('/calendar', roomController.calendar);
router.get('/:room_number', roomController.getById);
router.post('/', authorize('ADMIN'), roomController.create);
router.patch('/:room_number/status', roomController.setStatus);

module.exports = router;
