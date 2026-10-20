/**
 * Database Seed Script for Dhan-Gaadi Fleet OS
 * 
 * Run: node seed.js
 * 
 * Creates:
 * - 1 Admin user (mobile: 9999999999)
 * - 5 Buses
 * - 5 Routes  
 * - 10 Schedules
 */

require('dotenv').config();
const mongoose = require('mongoose');
const User = require('./models/User');
const Bus = require('./models/Bus');
const Route = require('./models/Route');
const Schedule = require('./models/Schedule');

const MONGO_URI = process.env.MONGO_URI || 'mongodb://localhost:27017/dhan-gaadi';

async function seed() {
  try {
    await mongoose.connect(MONGO_URI);
    console.log('✅ Connected to MongoDB');

    // Clear existing data
    await Promise.all([
      User.deleteMany({}),
      Bus.deleteMany({}),
      Route.deleteMany({}),
      Schedule.deleteMany({}),
    ]);
    console.log('🗑️  Cleared existing data');

    // 1. Create Admin User
    const admin = await User.create({
      name: 'Admin',
      mobile: '9999999999',
      role: 'admin',
      isVerified: true,
    });
    console.log('👤 Admin created: mobile=9999999999');

    // Create a test user
    const testUser = await User.create({
      name: 'Test Passenger',
      mobile: '9876543210',
      role: 'user',
      isVerified: true,
    });
    console.log('👤 Test user created: mobile=9876543210');

    // 2. Create Buses
    const buses = await Bus.insertMany([
      { busNumber: 'HR-38-A-1001', name: 'Hisar Express', type: 'deluxe', totalSeats: 45, depot: 'Hisar', amenities: ['Charging', 'Fan'] },
      { busNumber: 'HR-38-A-1002', name: 'Chandigarh Volvo', type: 'volvo-ac', totalSeats: 42, depot: 'Chandigarh', amenities: ['AC', 'WiFi', 'Charging', 'Curtains'] },
      { busNumber: 'HR-72-B-2001', name: 'Delhi Ordinary', type: 'ordinary', totalSeats: 52, depot: 'Rohtak', amenities: [] },
      { busNumber: 'HR-44-C-3001', name: 'Ambala Super', type: 'super-deluxe', totalSeats: 40, depot: 'Ambala', amenities: ['AC', 'WiFi', 'Charging', 'Recliner'] },
      { busNumber: 'HR-16-D-4001', name: 'Jind Ordinary', type: 'ordinary', totalSeats: 52, depot: 'Jind', amenities: ['Fan'] },
    ]);
    console.log(`🚌 ${buses.length} buses created`);

    // 3. Create Routes
    const routes = await Route.insertMany([
      {
        routeNumber: 'HR-38', name: 'Hisar - Chandigarh', source: 'Hisar', destination: 'Chandigarh',
        sourceDepot: 'Hisar Bus Stand', destinationDepot: 'ISBT Sector 43, Chandigarh',
        totalDistance: 280, baseFare: 450,
        stops: [
          { name: 'Hansi', distanceFromSource: 26, order: 1 },
          { name: 'Jind', distanceFromSource: 80, order: 2 },
          { name: 'Karnal', distanceFromSource: 170, order: 3 },
          { name: 'Ambala', distanceFromSource: 230, order: 4 },
        ]
      },
      {
        routeNumber: 'HR-72', name: 'Hisar - Delhi', source: 'Hisar', destination: 'Delhi',
        sourceDepot: 'Hisar Bus Stand', destinationDepot: 'Kashmere Gate ISBT, Delhi',
        totalDistance: 225, baseFare: 320,
        stops: [
          { name: 'Hansi', distanceFromSource: 26, order: 1 },
          { name: 'Rohtak', distanceFromSource: 110, order: 2 },
          { name: 'Bahadurgarh', distanceFromSource: 185, order: 3 },
        ]
      },
      {
        routeNumber: 'HR-44', name: 'Ambala - Delhi', source: 'Ambala', destination: 'Delhi',
        sourceDepot: 'Ambala Cantt Bus Stand', destinationDepot: 'ISBT Delhi',
        totalDistance: 200, baseFare: 380,
        stops: [
          { name: 'Karnal', distanceFromSource: 60, order: 1 },
          { name: 'Panipat', distanceFromSource: 105, order: 2 },
          { name: 'Sonipat', distanceFromSource: 155, order: 3 },
        ]
      },
      {
        routeNumber: 'HR-58', name: 'Rohtak - Chandigarh', source: 'Rohtak', destination: 'Chandigarh',
        sourceDepot: 'Rohtak Bus Stand', destinationDepot: 'ISBT Chandigarh',
        totalDistance: 250, baseFare: 420,
        stops: [
          { name: 'Jind', distanceFromSource: 60, order: 1 },
          { name: 'Kurukshetra', distanceFromSource: 150, order: 2 },
          { name: 'Ambala', distanceFromSource: 200, order: 3 },
        ]
      },
      {
        routeNumber: 'HR-16', name: 'Hisar - Rohtak', source: 'Hisar', destination: 'Rohtak',
        sourceDepot: 'Hisar Bus Stand', destinationDepot: 'Rohtak Bus Stand',
        totalDistance: 110, baseFare: 200,
        stops: [
          { name: 'Hansi', distanceFromSource: 26, order: 1 },
          { name: 'Jind', distanceFromSource: 80, order: 2 },
        ]
      },
    ]);
    console.log(`🛣️  ${routes.length} routes created`);

    // 4. Create Schedules
    const allDays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'];

    const schedules = await Schedule.insertMany([
      // Hisar - Chandigarh (2 schedules)
      { route: routes[0]._id, bus: buses[0]._id, departureTime: '06:30', arrivalTime: '12:00', daysOfOperation: allDays, fare: 450 },
      { route: routes[0]._id, bus: buses[1]._id, departureTime: '14:00', arrivalTime: '19:30', daysOfOperation: allDays, fare: 650 },
      
      // Hisar - Delhi (2 schedules)
      { route: routes[1]._id, bus: buses[2]._id, departureTime: '05:00', arrivalTime: '09:00', daysOfOperation: allDays, fare: 320 },
      { route: routes[1]._id, bus: buses[0]._id, departureTime: '16:00', arrivalTime: '20:00', daysOfOperation: weekdays, fare: 350 },
      
      // Ambala - Delhi (2 schedules)
      { route: routes[2]._id, bus: buses[3]._id, departureTime: '07:00', arrivalTime: '11:00', daysOfOperation: allDays, fare: 380 },
      { route: routes[2]._id, bus: buses[2]._id, departureTime: '15:30', arrivalTime: '19:30', daysOfOperation: allDays, fare: 350 },
      
      // Rohtak - Chandigarh (2 schedules)
      { route: routes[3]._id, bus: buses[0]._id, departureTime: '08:00', arrivalTime: '13:30', daysOfOperation: weekdays, fare: 420 },
      { route: routes[3]._id, bus: buses[1]._id, departureTime: '11:00', arrivalTime: '16:30', daysOfOperation: allDays, fare: 580 },
      
      // Hisar - Rohtak (2 schedules)
      { route: routes[4]._id, bus: buses[4]._id, departureTime: '06:00', arrivalTime: '08:30', daysOfOperation: allDays, fare: 200 },
      { route: routes[4]._id, bus: buses[4]._id, departureTime: '17:00', arrivalTime: '19:30', daysOfOperation: allDays, fare: 200 },
    ]);
    console.log(`📅 ${schedules.length} schedules created`);

    console.log('\n✅ ===== SEED COMPLETE =====');
    console.log('Admin login: mobile=9999999999');
    console.log('Test user: mobile=9876543210');
    console.log('OTP will be logged to console in dev mode');
    console.log('========================\n');

    process.exit(0);
  } catch (err) {
    console.error('❌ Seed error:', err);
    process.exit(1);
  }
}

seed();
