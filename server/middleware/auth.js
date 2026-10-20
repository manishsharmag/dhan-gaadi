const jwt = require('jsonwebtoken');
const User = require('../models/User');
const { error } = require('../utils/response');
const { getJwtSecret } = require('../helpers/jwtSecret');

const ADMIN_ROLES = ['admin', 'owner', 'superadmin'];
const STAFF_ROLES = ['admin', 'owner', 'superadmin', 'conductor'];

/**
 * Protect routes — verify JWT. In non-production, an optional fallback
 * can attach an existing user when `DEV_AUTH_FALLBACK=true` to ease local dev.
 */
const protect = async (req, res, next) => {
  try {
    const secret = getJwtSecret();
    if (!secret) return error(res, 'Server misconfiguration', 500);

    let token;
    if (req.headers.authorization && req.headers.authorization.startsWith('Bearer')) {
      token = req.headers.authorization.split(' ')[1];
    }

    if (token) {
      try {
        const decoded = jwt.verify(token, secret);
        const user = await User.findById(decoded.id);
        if (!user) return error(res, 'User not found', 401);
        req.user = user;
        return next();
      } catch (err) {
        return error(res, 'Invalid or expired token', 401);
      }
    }

    // No token: allow optional dev fallback when explicitly enabled
    const allowFallback = process.env.NODE_ENV !== 'production' && process.env.DEV_AUTH_FALLBACK === 'true';
    if (allowFallback) {
      const fallbackUser = await User.findOne({ role: 'owner' })
        || await User.findOne({ role: 'admin' })
        || await User.findOne({});
      if (fallbackUser) {
        req.user = fallbackUser;
        return next();
      }
    }

    return error(res, 'Not authorized, token missing', 401);
  } catch (err) {
    console.error('Auth protect error', err);
    return error(res, 'Authorization error', 500);
  }
};

const adminOnly = (req, res, next) => {
  if (!req.user) return error(res, 'Not authenticated', 401);
  if (!ADMIN_ROLES.includes(req.user.role)) return error(res, 'Admin access required', 403);
  return next();
};

const staffOnly = (req, res, next) => {
  if (!req.user) return error(res, 'Not authenticated', 401);
  if (!STAFF_ROLES.includes(req.user.role)) return error(res, 'Staff access required', 403);
  return next();
};

const ownerOnly = (req, res, next) => {
  if (!req.user) return error(res, 'Not authenticated', 401);
  if (!['owner', 'superadmin'].includes(req.user.role)) return error(res, 'Owner access required', 403);
  return next();
};

const anyAdmin = adminOnly;

module.exports = { protect, adminOnly, staffOnly, ownerOnly, anyAdmin };
