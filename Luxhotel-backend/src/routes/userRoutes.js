const router = require('express').Router();
const multer = require('multer');
const path = require('path');
const userController = require('../controllers/userController');
const { authenticate, authorize } = require('../middleware/auth');

const upload = multer({ dest: path.join(__dirname, '../../uploads') });

router.use(authenticate);

router.get('/', authorize('ADMIN'), userController.getAll);
router.get('/:username', authorize('ADMIN'), userController.getById);
router.post('/', authorize('ADMIN'), userController.create);
router.put('/:username', userController.update); // tự sửa hồ sơ của mình hoặc Admin sửa
router.patch('/:username/password', userController.changePassword);
router.patch('/:username/status', authorize('ADMIN'), userController.setStatus);
router.patch('/:username/avatar', upload.single('avatar'), userController.updateAvatar);

module.exports = router;
