const mongoose = require('mongoose');
const seedDatabase = require('./autoSeed');

const dbConnection = async () => {
  const uri = process.env.MONGO_URI;
  
  if (!uri) {
    console.error('[DB] FATAL ERROR: MONGO_URI is not defined in environment variables.');
    process.exit(1);
  }

  try {
    console.log(`[DB] Attempting to connect to MongoDB Atlas...`);
    // Using standard Mongoose connection timeouts (30s) suitable for cloud databases
    await mongoose.connect(uri);
    console.log(`[DB] MongoDB connected successfully!`);
    
    // Trigger auto-seeding
    await seedDatabase();
  } catch (err) {
    console.error(`[DB] FATAL ERROR: Failed to connect to MongoDB Atlas: ${err.message}`);
    // In production, it's safer to terminate the process and let the host restart it, rather than silently failing
    process.exit(1);
  }
};

module.exports = dbConnection;