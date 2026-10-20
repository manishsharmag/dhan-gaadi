/**
 * Centralized JWT Secret Resolver
 * Enforces strict presence of JWT_SECRET in production.
 */
const getJwtSecret = () => {
  const secret = process.env.JWT_SECRET;
  if (!secret) {
    if (process.env.NODE_ENV === 'production') {
      console.error('[FATAL] JWT_SECRET is required in production environment.');
      throw new Error('FATAL: JWT_SECRET environment variable is missing.');
    }
    return 'dev-secret-please-change';
  }
  return secret;
};

module.exports = { getJwtSecret };
