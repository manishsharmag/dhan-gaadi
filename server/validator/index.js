const { error } = require('../utils/response');

exports.userSignupValidator = (req, res, next) => {
  const { mobile, password } = req.body || {};
  if (!mobile || !/^[6-9]\d{9}$/.test(mobile)) {
    return error(res, 'Please enter a valid 10-digit Indian mobile number', 400);
  }
  if (!password || password.length < 6) {
    return error(res, 'Password must be at least 6 characters long', 400);
  }
  next();
};

exports.passwordResetValidator = (req, res, next) => {
  const { newPassword } = req.body || {};
  if (!newPassword || newPassword.length < 6) {
    return error(res, 'New password must be at least 6 characters long', 400);
  }
  next();
};