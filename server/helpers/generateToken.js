const jwt = require('jsonwebtoken');
const { getJwtSecret } = require('./jwtSecret');

const generateToken = (user) => {
  return jwt.sign(
    { id: user._id, mobile: user.mobile, role: user.role, name: user.name },
    getJwtSecret(),
    { expiresIn: '30d' }
  );
};

module.exports = generateToken;
