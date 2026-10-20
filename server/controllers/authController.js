const User = require('../models/User');
const generateToken = require('../helpers/generateToken');
const bcrypt = require('bcryptjs');
const { success, error } = require('../utils/response');

/**
 * Register a new user
 */
const register = async (req, res) => {
  try {
    const { mobile, name, password } = req.body;
    
    if (!mobile || !/^[6-9]\d{9}$/.test(mobile)) {
      return error(res, 'Please enter a valid 10-digit Indian mobile number', 400);
    }

    if (!password || password.length < 6) {
      return error(res, 'Password must be at least 6 characters long', 400);
    }
    
    let user = await User.findOne({ mobile });
    if (user) {
      return error(res, 'User with this mobile number already exists', 400);
    }

    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(password, salt);
    
    user = new User({ 
      mobile, 
      name: name || '',
      password: hashedPassword 
    });
    
    await user.save();
    
    const token = generateToken(user);
    
    return success(res, {
      token,
      user: {
        id: user._id,
        mobile: user.mobile,
        name: user.name,
        role: user.role,
      }
    }, 'Registration successful');
  } catch (err) {
    console.error('[AUTH] register error:', err);
    return error(res, 'Registration failed. Please try again.', 500);
  }
};

/**
 * Login with mobile and password
 */
const login = async (req, res) => {
  try {
    const { mobile, password } = req.body;
    
    if (!mobile || !password) {
      return error(res, 'Mobile number and password are required', 400);
    }

    const user = await User.findOne({ mobile }).select('+password');
    if (!user) {
      return error(res, 'Invalid mobile number or password', 401);
    }

    if (!user.isActive) {
      return error(res, 'Your account has been deactivated.', 403);
    }
    
    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) {
      return error(res, 'Invalid mobile number or password', 401);
    }
    
    const token = generateToken(user);
    
    return success(res, {
      token,
      user: {
        id: user._id,
        mobile: user.mobile,
        name: user.name,
        role: user.role,
      }
    }, 'Login successful');
  } catch (err) {
    console.error('[AUTH] login error:', err);
    return error(res, 'Login failed. Please try again.', 500);
  }
};

/**
 * Change or reset password
 */
const resetPassword = async (req, res) => {
  try {
    const { mobile, currentPassword, newPassword } = req.body;
    if (!newPassword || newPassword.length < 6) {
      return error(res, 'New password must be at least 6 characters', 400);
    }

    // If authenticated user is updating their own password
    if (req.user) {
      if (!currentPassword) {
        return error(res, 'Current password is required', 400);
      }
      const user = await User.findById(req.user._id).select('+password');
      if (!user) return error(res, 'User not found', 404);

      const isMatch = await bcrypt.compare(currentPassword, user.password);
      if (!isMatch) return error(res, 'Current password does not match', 400);

      const salt = await bcrypt.genSalt(10);
      user.password = await bcrypt.hash(newPassword, salt);
      await user.save();

      return success(res, null, 'Password updated successfully');
    }

    // Unauthenticated reset request
    if (!mobile || !currentPassword) {
      return error(res, 'Mobile number, current password, and new password are required', 400);
    }

    const cleanMobile = mobile.replace(/\D/g, '').slice(-10);
    const user = await User.findOne({ 
      $or: [{ mobile: cleanMobile }, { mobile }] 
    }).select('+password');

    if (!user) return error(res, 'No account found with this mobile number', 404);

    const isMatch = await bcrypt.compare(currentPassword, user.password);
    if (!isMatch) return error(res, 'Current password does not match', 400);

    const salt = await bcrypt.genSalt(10);
    user.password = await bcrypt.hash(newPassword, salt);
    await user.save();

    return success(res, null, 'Password reset successfully. You can now login with your new password.');
  } catch (err) {
    console.error('[AUTH] resetPassword error:', err);
    return error(res, 'Password update failed', 500);
  }
};

/**
 * Get current user profile
 */
const getProfile = async (req, res) => {
  try {
    const user = await User.findById(req.user._id);
    if (!user) {
      return error(res, 'User not found', 404);
    }
    return success(res, {
      id: user._id,
      mobile: user.mobile,
      name: user.name,
      role: user.role,
    });
  } catch (err) {
    return error(res, 'Failed to fetch profile', 500);
  }
};

/**
 * Update user profile (name)
 */
const updateProfile = async (req, res) => {
  try {
    const { name } = req.body;
    const user = await User.findByIdAndUpdate(
      req.user._id,
      { name },
      { new: true }
    );
    if (!user) {
      return error(res, 'User not found', 404);
    }
    return success(res, {
      id: user._id,
      mobile: user.mobile,
      name: user.name,
      role: user.role,
    }, 'Profile updated');
  } catch (err) {
    return error(res, 'Failed to update profile', 500);
  }
};

/**
 * Create a Conductor Account (Owner only)
 */
const createConductor = async (req, res) => {
  try {
    const { mobile, name, password } = req.body || {};
    if (!mobile || !password) return error(res, 'Mobile and password required', 400);

    const cleanMobile = String(mobile).replace(/\D/g, '').slice(-10);
    if (!/^[6-9]\d{9}$/.test(cleanMobile)) {
      return error(res, 'Please enter a valid 10-digit Indian mobile number', 400);
    }

    if (password.length < 6) {
      return error(res, 'Password must be at least 6 characters long', 400);
    }

    const existingUser = await User.findOne({
      $or: [{ mobile: cleanMobile }, { mobile }]
    });
    if (existingUser) return error(res, 'User with this mobile number already exists', 400);

    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(password, salt);
    
    const conductor = new User({ mobile: cleanMobile, name: (name || '').trim(), password: hashedPassword, role: 'conductor' });
    await conductor.save();
    
    return success(res, { id: conductor._id, mobile: conductor.mobile, name: conductor.name }, 'Conductor created', 201);
  } catch(err) {
    return error(res, 'Failed to create conductor', 500);
  }
};

/**
 * Get all Conductors (Owner / Admin)
 */
const getConductors = async (req, res) => {
  try {
    const conductors = await User.find({ role: 'conductor' }).select('-password').sort({ createdAt: -1 });
    return success(res, conductors);
  } catch(err) {
    return error(res, 'Failed to fetch conductors', 500);
  }
};

/**
 * Delete a Conductor (Owner / Admin)
 */
const deleteConductor = async (req, res) => {
  try {
    const { id } = req.params;
    const user = await User.findOneAndDelete({ _id: id, role: 'conductor' });
    if (!user) return error(res, 'Conductor not found', 404);
    return success(res, null, 'Conductor removed successfully');
  } catch (err) {
    return error(res, 'Failed to delete conductor', 500);
  }
};

module.exports = { register, login, resetPassword, getProfile, updateProfile, createConductor, getConductors, deleteConductor };
