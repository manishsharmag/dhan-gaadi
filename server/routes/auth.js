const express = require('express');
const router = express.Router();
const { register, login, resetPassword, getProfile, updateProfile, createConductor, getConductors, deleteConductor } = require('../controllers/authController');
const { protect, anyAdmin } = require('../middleware/auth');
const { apiLimiter } = require('../middleware/rateLimiter');
const { userSignupValidator, passwordResetValidator } = require('../validator');

// Public — Password Auth flow
router.post('/register', apiLimiter, userSignupValidator, register);
router.post('/login', apiLimiter, login);
router.post('/reset-password', apiLimiter, passwordResetValidator, resetPassword);

// Protected — profile
router.get('/profile', protect, getProfile);
router.put('/profile', protect, updateProfile);

// Admin / Owner - Conductor Management
router.post('/admin/conductors', protect, anyAdmin, createConductor);
router.get('/admin/conductors', protect, anyAdmin, getConductors);
router.delete('/admin/conductors/:id', protect, anyAdmin, deleteConductor);

module.exports = router;
