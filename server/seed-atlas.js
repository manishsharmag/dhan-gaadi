// Manual seed script for MongoDB Atlas
// Usage: node seed-atlas.js
// Run this on your deployed backend to seed production database

const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
require('dotenv').config();

const User = require('./models/User');
const Bus = require('./models/Bus');
const Route = require('./models/Route');
const Schedule = require('./models/Schedule');
const QRCode = require('./models/QRCode');

const seedAtlas = async () => {
  try {
    // Connect to Atlas MongoDB
    const mongoUri = process.env.MONGO_URI;
    if (!mongoUri) {
      console.error('ERROR: MONGO_URI not set in environment');
      process.exit(1);
    }

    console.log('[SEED] Connecting to MongoDB Atlas...');
    await mongoose.connect(mongoUri);
    console.log('[SEED] Connected successfully');

    // 1. Seed Users
    let userCount = await User.countDocuments();
    if (userCount === 0) {
      const ownerMobile = process.env.SEED_OWNER_MOBILE || '9991672565';
      const ownerPass = process.env.SEED_OWNER_PASSWORD || (process.env.NODE_ENV === 'production' ? null : 'Ms@9916');
      const adminMobile = process.env.SEED_ADMIN_MOBILE || '9416618344';
      const adminPass = process.env.SEED_ADMIN_PASSWORD || (process.env.NODE_ENV === 'production' ? null : 'Admin@12345');
      const userPass = process.env.SEED_USER_PASSWORD || 'password123';

      if (!ownerPass || !adminPass) {
        console.warn('[SEED] Please set SEED_OWNER_PASSWORD and SEED_ADMIN_PASSWORD for seeding production.');
      } else {
        const ownerSalt = await bcrypt.genSalt(10);
        const ownerPassword = await bcrypt.hash(ownerPass, ownerSalt);
        const userSalt = await bcrypt.genSalt(10);
        const userPassword = await bcrypt.hash(userPass, userSalt);
        const adminSalt = await bcrypt.genSalt(10);
        const adminPassword = await bcrypt.hash(adminPass, adminSalt);

        await User.create([
          {
            name: 'System Owner',
            mobile: ownerMobile,
            password: ownerPassword,
            role: 'owner',
            isActive: true
          },
          {
            name: 'Admin User',
            mobile: adminMobile,
            password: adminPassword,
            role: 'admin',
            isActive: true
          },
          {
            name: 'Test Passenger',
            mobile: '9876543210',
            password: userPassword,
            role: 'user',
            isActive: true
          },
          {
            name: 'Conductor Test',
            mobile: '9999999999',
            password: userPassword,
            role: 'conductor',
            isActive: true
          }
        ]);
        console.log('[SEED] Users created successfully');
      }
    } else {
      console.log(`[SEED] Users already exist (${userCount})`);
    }

    // 2. Seed Buses
    let busCount = await Bus.countDocuments();
    if (busCount === 0) {
      await Bus.insertMany([
        {
          busNumber: 'HR-38-A-1001',
          name: 'Hisar Express',
          type: 'deluxe',
          totalSeats: 45,
          depot: 'Hisar',
          amenities: ['Charging', 'Fan', 'Push-back Seats'],
          images: ['https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=800']
        },
        {
          busNumber: 'HR-38-A-1002',
          name: 'Chandigarh Volvo AC',
          type: 'volvo-ac',
          totalSeats: 42,
          depot: 'Chandigarh',
          amenities: ['AC', 'WiFi', 'Charging'],
          images: ['https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=800']
        },
        {
          busNumber: 'HR-72-B-2001',
          name: 'Delhi Ordinary',
          type: 'ordinary',
          totalSeats: 52,
          depot: 'Rohtak',
          amenities: ['Fan'],
          images: ['https://images.unsplash.com/photo-1494515843206-f3117d3f51b7?w=800']
        },
        {
          busNumber: 'HR-44-C-3001',
          name: 'Ambala Super Deluxe',
          type: 'super-deluxe',
          totalSeats: 40,
          depot: 'Ambala',
          amenities: ['AC', 'WiFi', 'Charging', 'Recliner'],
          images: ['https://images.unsplash.com/photo-1562620644-65bb4d03e12f?w=800']
        },
        {
          busNumber: 'HR-16-D-4001',
          name: 'Jind Local Express',
          type: 'ordinary',
          totalSeats: 52,
          depot: 'Jind',
          amenities: ['Fan'],
          images: ['https://images.unsplash.com/photo-1494515843206-f3117d3f51b7?w=800']
        }
      ]);
      console.log('[SEED] Buses created');
    } else {
      console.log(`[SEED] Buses already exist (${busCount})`);
    }

    // 3. Seed Routes
    let routeCount = await Route.countDocuments();
    if (routeCount === 0) {
      await Route.insertMany([
        {
          routeNumber: 'HR-38',
          name: 'Hisar - Chandigarh Express',
          source: 'Hisar',
          destination: 'Chandigarh',
          sourceDepot: 'Hisar Bus Stand',
          destinationDepot: 'ISBT Sector 43, Chandigarh',
          totalDistance: 280,
          baseFare: 450,
          images: ['https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=800'],
          stops: [
            { name: 'Hansi', distanceFromSource: 26, order: 1 },
            { name: 'Jind', distanceFromSource: 80, order: 2 }
          ]
        },
        {
          routeNumber: 'HR-72',
          name: 'Hisar - Delhi Direct',
          source: 'Hisar',
          destination: 'Delhi',
          sourceDepot: 'Hisar Bus Stand',
          destinationDepot: 'Kashmere Gate ISBT, Delhi',
          totalDistance: 225,
          baseFare: 320,
          images: ['https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=800'],
          stops: []
        },
        {
          routeNumber: 'HR-74',
          name: 'Karnal - Rohtak Local',
          source: 'Karnal',
          destination: 'Rohtak',
          sourceDepot: 'Karnal Bus Stand',
          destinationDepot: 'Rohtak Bus Stand',
          totalDistance: 90,
          baseFare: 380,
          images: ['https://images.unsplash.com/photo-1494515843206-f3117d3f51b7?w=800'],
          stops: []
        },
        {
          routeNumber: 'HR-44',
          name: 'Ambala - Panchkula Express',
          source: 'Ambala',
          destination: 'Panchkula',
          sourceDepot: 'Ambala Bus Stand',
          destinationDepot: 'Panchkula Bus Stand',
          totalDistance: 50,
          baseFare: 420,
          images: ['https://images.unsplash.com/photo-1562620644-65bb4d03e12f?w=800'],
          stops: []
        },
        {
          routeNumber: 'HR-16',
          name: 'Jind - Rohtak Local',
          source: 'Jind',
          destination: 'Rohtak',
          sourceDepot: 'Jind Bus Stand',
          destinationDepot: 'Rohtak Bus Stand',
          totalDistance: 70,
          baseFare: 200,
          images: ['https://images.unsplash.com/photo-1494515843206-f3117d3f51b7?w=800'],
          stops: []
        }
      ]);
      console.log('[SEED] Routes created');
    } else {
      console.log(`[SEED] Routes already exist (${routeCount})`);
    }

    // 4. Seed Schedules with FARE (FIX for fare showing 0)
    let scheduleCount = await Schedule.countDocuments();
    if (scheduleCount === 0) {
      const routes = await Route.find({});
      const buses = await Bus.find({});

      if (routes.length > 0 && buses.length > 0) {
        const allDays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
        const weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'];

        await Schedule.insertMany([
          { route: routes[0]._id, bus: buses[0]._id, departureTime: '06:30', arrivalTime: '12:00', daysOfOperation: allDays, fare: 450 },
          { route: routes[0]._id, bus: buses[1]._id, departureTime: '14:00', arrivalTime: '19:30', daysOfOperation: allDays, fare: 650 },
          { route: routes[1]._id, bus: buses[2]._id, departureTime: '05:00', arrivalTime: '09:00', daysOfOperation: allDays, fare: 320 },
          { route: routes[1]._id, bus: buses[0]._id, departureTime: '16:00', arrivalTime: '20:00', daysOfOperation: weekdays, fare: 350 },
          { route: routes[2]._id, bus: buses[3]._id, departureTime: '07:00', arrivalTime: '11:00', daysOfOperation: allDays, fare: 380 },
          { route: routes[2]._id, bus: buses[2]._id, departureTime: '15:30', arrivalTime: '19:30', daysOfOperation: allDays, fare: 350 },
          { route: routes[3]._id, bus: buses[0]._id, departureTime: '08:00', arrivalTime: '13:30', daysOfOperation: weekdays, fare: 420 },
          { route: routes[3]._id, bus: buses[1]._id, departureTime: '11:00', arrivalTime: '16:30', daysOfOperation: allDays, fare: 580 },
          { route: routes[4]._id, bus: buses[4]._id, departureTime: '06:00', arrivalTime: '08:30', daysOfOperation: allDays, fare: 200 },
          { route: routes[4]._id, bus: buses[4]._id, departureTime: '17:00', arrivalTime: '19:30', daysOfOperation: allDays, fare: 200 }
        ]);
        console.log('[SEED] Schedules created with FARE data');
      }
    } else {
      console.log(`[SEED] Schedules already exist (${scheduleCount})`);
      // FIX: Ensure all schedules have fare set
      const schedulesWithoutFare = await Schedule.find({ fare: { $exists: false } });
      if (schedulesWithoutFare.length > 0) {
        console.log(`[SEED] WARNING: Found ${schedulesWithoutFare.length} schedules without fare. Updating...`);
        for (const sched of schedulesWithoutFare) {
          const route = await Route.findById(sched.route);
          sched.fare = route?.baseFare || 400;
          await sched.save();
        }
        console.log('[SEED] Schedules updated with default fares');
      }
    }

    // 5. Seed QR Code
    let qrCount = await QRCode.countDocuments();
    if (qrCount === 0) {
      const owner = await User.findOne({ role: 'owner' });
      await QRCode.create({
        upiId: 'fleet@dhangaadi',
        upiLabel: 'Dhan-Gaadi Fleet UPI',
        imagePath: '/img/qr.png',
        isActive: true,
        uploadedBy: owner._id
      });
      console.log('[SEED] QR Code created');
    } else {
      console.log(`[SEED] QR Code already exists`);
    }

    console.log('[SEED] ===== ATLAS SEED COMPLETE =====');
    process.exit(0);
  } catch (err) {
    console.error('[SEED] Error:', err.message);
    process.exit(1);
  }
};

seedAtlas();
