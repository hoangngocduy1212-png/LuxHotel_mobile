const router = require('express').Router();
const bookingController = require('../controllers/bookingController');
const { authenticate, authorize } = require('../middleware/auth');

router.use(authenticate);

router.get('/', bookingController.getByStatus);
router.get('/:id', bookingController.getById);
router.post('/', authorize('ADMIN', 'RECEPTIONIST'), bookingController.create);
router.post('/:id/checkin', authorize('ADMIN', 'RECEPTIONIST'), bookingController.checkin);
router.post('/:id/checkout', authorize('ADMIN', 'RECEPTIONIST'), bookingController.checkout);
router.post('/:id/service-charges', authorize('ADMIN', 'RECEPTIONIST'), bookingController.addServiceCharge);

module.exports = router;
