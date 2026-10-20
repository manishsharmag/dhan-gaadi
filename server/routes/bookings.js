const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const { protect, adminOnly, staffOnly } = require('../middleware/auth');
const {
  getSeatAvailability,
  createBooking,
  submitPayment,
  instantDemoPayment,
  getPendingBookings,
  confirmBooking,
  rejectBooking,
  cancelBooking,
  getAllBookings,
  getUserBookings,
  getBooking,
  getBookingByPNR,
  downloadTicketPDF,
  getDashboardStats,
  exportBookingsCSV,
} = require('../controllers/bookingController');

// Multer config for payment screenshot uploads
const uploadPath = path.join(__dirname, '..', 'public', 'uploads');
if (!fs.existsSync(uploadPath)) {
  fs.mkdirSync(uploadPath, { recursive: true });
}

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadPath);
  },
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname || '') || '.jpg';
    const uniqueName = `payment-${Date.now()}-${Math.round(Math.random() * 1E9)}${ext}`;
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

// Public
router.get('/pnr/:pnr', getBookingByPNR);
router.get('/ticket/:pnr', downloadTicketPDF);
router.get('/download/:pnr', downloadTicketPDF);
router.get('/seats', getSeatAvailability);

// User (protected)
router.post('/', protect, createBooking);
router.post('/:bookingId/payment', protect, upload.single('screenshot'), submitPayment);
router.post('/:bookingId/instant-demo-pay', protect, instantDemoPayment);
router.put('/:bookingId/cancel', protect, cancelBooking);
router.get('/my-bookings', protect, getUserBookings);
router.get('/:id', protect, getBooking);

// Admin Only (Conductor has dedicated My Duty portal and POS)
router.get('/admin/pending', protect, adminOnly, getPendingBookings);
router.get('/admin/all', protect, adminOnly, getAllBookings);
router.get('/admin/dashboard', protect, adminOnly, getDashboardStats);
router.put('/admin/:bookingId/confirm', protect, adminOnly, confirmBooking);
router.put('/admin/:bookingId/reject', protect, adminOnly, rejectBooking);

// Admin Only
router.get('/admin/export', protect, adminOnly, exportBookingsCSV);

module.exports = router;
