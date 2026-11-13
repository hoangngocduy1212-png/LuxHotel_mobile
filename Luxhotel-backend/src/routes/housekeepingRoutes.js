const router = require('express').Router();
const housekeepingController = require('../controllers/housekeepingController');
const { authenticate, authorize } = require('../middleware/auth');

router.use(authenticate);

router.get('/', housekeepingController.getAll);
router.get('/summary', housekeepingController.summary);
router.post('/', authorize('ADMIN', 'RECEPTIONIST'), housekeepingController.create);
router.post('/periodic-check', authorize('ADMIN'), housekeepingController.periodicCheck);
router.patch('/:id/assign', authorize('ADMIN', 'RECEPTIONIST'), housekeepingController.assign);
router.patch('/:id/done', housekeepingController.setDone);

module.exports = router;
