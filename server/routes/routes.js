const express = require('express');
const router = express.Router();
const { protect, anyAdmin } = require('../middleware/auth');
const { addRoute, getRoutes, getRoute, updateRoute, deleteRoute, searchRoutes, getCities } = require('../controllers/routeController');

// Public
router.get('/cities', getCities);
router.get('/search', searchRoutes);
router.get('/', getRoutes);
router.get('/:id', getRoute);

// Admin (Owner, Admin, Superadmin)
router.post('/', protect, anyAdmin, addRoute);
router.put('/:id', protect, anyAdmin, updateRoute);
router.delete('/:id', protect, anyAdmin, deleteRoute);

module.exports = router;
