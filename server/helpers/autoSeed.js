const bcrypt = require('bcryptjs');
const User = require('../models/User');
const Bus = require('../models/Bus');
const Route = require('../models/Route');
const Schedule = require('../models/Schedule');
const QRCode = require('../models/QRCode');

const seedDatabase = async () => {
  try {
    console.log('[DB] Checking production seed status...');

    // 1. Seed Users if missing
    let owner = await User.findOne({ role: 'owner' });
    if (!owner) {
      const ownerMobile = process.env.SEED_OWNER_MOBILE || '9991672565';
      const ownerPass = process.env.SEED_OWNER_PASSWORD || (process.env.NODE_ENV === 'production' ? null : 'Ms@9916');
      const adminMobile = process.env.SEED_ADMIN_MOBILE || '9416618344';
      const adminPass = process.env.SEED_ADMIN_PASSWORD || (process.env.NODE_ENV === 'production' ? null : 'Admin@12345');
      const userPass = process.env.SEED_USER_PASSWORD || 'password123';

      if (!ownerPass || !adminPass) {
        console.warn('[DB] SEED_OWNER_PASSWORD and SEED_ADMIN_PASSWORD must be provided in production to seed administrative users.');
      } else {
        const ownerSalt = await bcrypt.genSalt(10);
        const ownerHashed = await bcrypt.hash(ownerPass, ownerSalt);
        const adminSalt = await bcrypt.genSalt(10);
        const adminHashed = await bcrypt.hash(adminPass, adminSalt);
        const userSalt = await bcrypt.genSalt(10);
        const userHashed = await bcrypt.hash(userPass, userSalt);

        owner = await User.create({
          name: 'System Owner',
          mobile: ownerMobile,
          password: ownerHashed,
          role: 'owner',
          isActive: true
        });

        await User.create({
          name: 'Admin User',
          mobile: adminMobile,
          password: adminHashed,
          role: 'admin',
          isActive: true
        });

        await User.create({
          name: 'Test Passenger',
          mobile: '9876543210',
          password: userHashed,
          role: 'user',
          isActive: true
        });

        await User.create({
          name: 'Conductor Test',
          mobile: '9999999999',
          password: userHashed,
          role: 'conductor',
          isActive: true
        });

        console.log('[DB] Users seeded successfully');
      }
    }

    // 2. Seed Buses if missing
    let buses = await Bus.find({});
    if (buses.length === 0) {
      buses = await Bus.insertMany([
        {
          busNumber: 'HR-38-A-1001',
          name: 'Hisar Express',
          type: 'deluxe',
          totalSeats: 45,
          depot: 'Hisar',
          amenities: ['Charging', 'Fan', 'Push-back Seats'],
          images: [
            'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=800',
            'https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=800'
          ]
        },
        {
          busNumber: 'HR-38-A-1002',
          name: 'Chandigarh Volvo AC',
          type: 'volvo-ac',
          totalSeats: 42,
          depot: 'Chandigarh',
          amenities: ['AC', 'WiFi', 'Charging', 'Curtains', 'Recliner'],
          images: [
            'https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=800',
            'https://images.unsplash.com/photo-1562620644-65bb4d03e12f?w=800'
          ]
        },
        {
          busNumber: 'HR-72-B-2001',
          name: 'Delhi Ordinary',
          type: 'ordinary',
          totalSeats: 52,
          depot: 'Rohtak',
          amenities: ['Fan'],
          images: [
            'https://images.unsplash.com/photo-1494515843206-f3117d3f51b7?w=800'
          ]
        },
        {
          busNumber: 'HR-44-C-3001',
          name: 'Ambala Super Deluxe',
          type: 'super-deluxe',
          totalSeats: 40,
          depot: 'Ambala',
          amenities: ['AC', 'WiFi', 'Charging', 'Recliner', 'Leg Rest'],
          images: [
            'https://images.unsplash.com/photo-1562620644-65bb4d03e12f?w=800',
            'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=800'
          ]
        },
        {
          busNumber: 'HR-16-D-4001',
          name: 'Jind Local Express',
          type: 'ordinary',
          totalSeats: 52,
          depot: 'Jind',
          amenities: ['Fan'],
          images: [
            'https://images.unsplash.com/photo-1494515843206-f3117d3f51b7?w=800'
          ]
        },
      ]);
      console.log(`[DB] ${buses.length} buses seeded`);
    }

    // 3. Seed Routes if missing
    let routes = await Route.find({});
    if (routes.length === 0) {
      routes = await Route.insertMany([
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
            { name: 'Jind', distanceFromSource: 80, order: 2 },
            { name: 'Karnal', distanceFromSource: 170, order: 3 },
            { name: 'Ambala', distanceFromSource: 230, order: 4 },
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
          stops: [
            { name: 'Hansi', distanceFromSource: 26, order: 1 },
            { name: 'Rohtak', distanceFromSource: 110, order: 2 },
            { name: 'Bahadurgarh', distanceFromSource: 185, order: 3 },
          ]
        },
        {
          routeNumber: 'HR-44',
          name: 'Ambala - Delhi Highway Super',
          source: 'Ambala',
          destination: 'Delhi',
          sourceDepot: 'Ambala Cantt Bus Stand',
          destinationDepot: 'ISBT Delhi',
          totalDistance: 200,
          baseFare: 380,
          images: ['https://images.unsplash.com/photo-1562620644-65bb4d03e12f?w=800'],
          stops: [
            { name: 'Karnal', distanceFromSource: 60, order: 1 },
            { name: 'Panipat', distanceFromSource: 105, order: 2 },
            { name: 'Sonipat', distanceFromSource: 155, order: 3 },
          ]
        },
        {
          routeNumber: 'HR-58',
          name: 'Rohtak - Chandigarh Express',
          source: 'Rohtak',
          destination: 'Chandigarh',
          sourceDepot: 'Rohtak Bus Stand',
          destinationDepot: 'ISBT Chandigarh',
          totalDistance: 250,
          baseFare: 420,
          images: ['https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=800'],
          stops: [
            { name: 'Jind', distanceFromSource: 60, order: 1 },
            { name: 'Kurukshetra', distanceFromSource: 150, order: 2 },
            { name: 'Ambala', distanceFromSource: 200, order: 3 },
          ]
        },
        {
          routeNumber: 'HR-16',
          name: 'Hisar - Rohtak Connector',
          source: 'Hisar',
          destination: 'Rohtak',
          sourceDepot: 'Hisar Bus Stand',
          destinationDepot: 'Rohtak Bus Stand',
          totalDistance: 110,
          baseFare: 200,
          images: ['https://images.unsplash.com/photo-1494515843206-f3117d3f51b7?w=800'],
          stops: [
            { name: 'Hansi', distanceFromSource: 26, order: 1 },
            { name: 'Jind', distanceFromSource: 80, order: 2 },
          ]
        },
      ]);
      console.log(`[DB] ${routes.length} routes seeded`);
    }

    // 4. Seed Schedules if missing
    const scheduleCount = await Schedule.countDocuments();
    if (scheduleCount === 0 && routes.length > 0 && buses.length > 0) {
      const allDays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
      const weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'];

      const schedules = await Schedule.insertMany([
        { route: routes[0]._id, bus: buses[0]._id, departureTime: '06:30', arrivalTime: '12:00', daysOfOperation: allDays, fare: 450 },
        { route: routes[0]._id, bus: buses[1]._id, departureTime: '14:00', arrivalTime: '19:30', daysOfOperation: allDays, fare: 650 },
        { route: routes[1]._id, bus: buses[2]._id, departureTime: '05:00', arrivalTime: '09:00', daysOfOperation: allDays, fare: 320 },
        { route: routes[1]._id, bus: buses[0]._id, departureTime: '16:00', arrivalTime: '20:00', daysOfOperation: weekdays, fare: 350 },
        { route: routes[2]._id, bus: buses[3]._id, departureTime: '07:00', arrivalTime: '11:00', daysOfOperation: allDays, fare: 380 },
        { route: routes[2]._id, bus: buses[2]._id, departureTime: '15:30', arrivalTime: '19:30', daysOfOperation: allDays, fare: 350 },
        { route: routes[3]._id, bus: buses[0]._id, departureTime: '08:00', arrivalTime: '13:30', daysOfOperation: weekdays, fare: 420 },
        { route: routes[3]._id, bus: buses[1]._id, departureTime: '11:00', arrivalTime: '16:30', daysOfOperation: allDays, fare: 580 },
        { route: routes[4]._id, bus: buses[4]._id, departureTime: '06:00', arrivalTime: '08:30', daysOfOperation: allDays, fare: 200 },
        { route: routes[4]._id, bus: buses[4]._id, departureTime: '17:00', arrivalTime: '19:30', daysOfOperation: allDays, fare: 200 },
      ]);
      console.log(`[DB] ${schedules.length} schedules seeded`);
    }

    // 5. Create active QR Code for payment if missing
    const qrCount = await QRCode.countDocuments();
    if (qrCount === 0) {
      await QRCode.create({
        upiId: 'fleet@dhangaadi',
        upiLabel: 'Dhan-Gaadi Fleet UPI',
        imagePath: '/img/qr.png',
        isActive: true,
        uploadedBy: owner._id
      });
      console.log('[DB] Default UPI QR code seeded');
    }

    console.log('[DB] ===== AUTO SEED COMPLETE =====');
  } catch (err) {
    console.error('[DB] Auto-seed failed:', err);
  }
};

module.exports = seedDatabase;
