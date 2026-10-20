const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const { protect, adminOnly, anyAdmin } = require('../middleware/auth');
const { createQRCode, getQRCodes, getActiveQRCode, setActiveQRCode, deleteQRCode } = require('../controllers/qrController');

const uploadPath = path.join(__dirname, '..', 'public', 'uploads');
if (!fs.existsSync(uploadPath)) {
  fs.mkdirSync(uploadPath, { recursive: true });
}

// Multer config for QR code image uploads
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadPath);
  },
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname || '') || '.jpg';
    const uniqueName = `qr-${Date.now()}-${Math.round(Math.random() * 1E9)}${ext}`;
    cb(null, uniqueName);
  },
});

const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB max
  fileFilter: (req, file, cb) => {
    const allowedMimes = ['image/jpeg', 'image/png', 'image/webp'];
    const allowedExts = /\.(jpe?g|png|webp)$/i;
    if (allowedMimes.includes(file.mimetype) && allowedExts.test(file.originalname || '')) {
      return cb(null, true);
    }
    cb(new Error('Only image files (JPEG, PNG, WebP) are allowed'));
  },
});

// Public — get active QR for payment
router.get('/active', getActiveQRCode);

// Admin (Owner, Superadmin, Admin)
router.get('/', protect, anyAdmin, getQRCodes);
router.post('/', protect, anyAdmin, upload.single('qrImage'), createQRCode);
router.put('/:id/activate', protect, anyAdmin, setActiveQRCode);
router.delete('/:id', protect, anyAdmin, deleteQRCode);

module.exports = router;
