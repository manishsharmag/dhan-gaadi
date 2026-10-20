const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  name: { type: String, default: '' },
  mobile: { 
    type: String, 
    required: true, 
    unique: true,
    match: [/^[6-9]\d{9}$/, 'Please enter a valid Indian mobile number']
  },
  role: { 
    type: String, 
    enum: ['user', 'admin', 'superadmin', 'owner', 'conductor'], 
    default: 'user' 
  },
  password: { 
    type: String, 
    required: true,
    select: false 
  },
  photo: { type: String, default: '' },
  isActive: { type: Boolean, default: true },
}, { timestamps: true });

// Index for OTP rate limiting lookups
userSchema.index({ mobile: 1 });

module.exports = mongoose.model('User', userSchema);
