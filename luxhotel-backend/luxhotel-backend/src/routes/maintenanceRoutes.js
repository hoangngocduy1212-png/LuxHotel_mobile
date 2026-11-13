const router = require('express').Router();
const multer = require('multer');
const path = require('path');
const maintenanceController = require('../controllers/maintenanceController');
const { authenticate, authorize } = require('../middleware/auth');

const upload = multer({ dest: path.join(__dirname, '../../uploads') });

router.use(authenticate);

router.get('/', maintenanceController.getAll);
router.post('/', upload.single('image'), maintenanceController.create);
router.patch('/:id/status', authorize('ADMIN', 'HOUSEKEEPING'), maintenanceController.setStatus);

module.exports = router;
