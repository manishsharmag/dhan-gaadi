const express = require('express');
const router = express.Router();
const { protect, anyAdmin, staffOnly } = require('../middleware/auth');
const {
  addSchedule,
  getSchedules,
  getSchedule,
  updateSchedule,
  deleteSchedule,
  getConductorDuty,
  searchSchedules,
} = require('../controllers/scheduleController');

// Public
router.get('/search', searchSchedules);
router.get('/', getSchedules);

// Conductor / Staff Duty Route (Must precede /:id)
router.get('/conductor/my-duty', protect, staffOnly, getConductorDuty);

// Single schedule lookup
router.get('/:id', getSchedule);

// Admin Only (Owner, Admin, Superadmin)
router.post('/', protect, anyAdmin, addSchedule);
router.put('/:id', protect, anyAdmin, updateSchedule);
router.delete('/:id', protect, anyAdmin, deleteSchedule);

module.exports = router;
