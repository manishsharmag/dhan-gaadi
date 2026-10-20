const Schedule = require('../models/Schedule');
const Route = require('../models/Route');
const Bus = require('../models/Bus');
const User = require('../models/User');
const Booking = require('../models/Booking');
const BusTracking = require('../models/BusTracking');
const dayjs = require('dayjs');
const { success, error } = require('../utils/response');

const addSchedule = async (req, res) => {
  try {
    const { route, bus, departureTime, arrivalTime, daysOfOperation, fare, conductor } = req.body;
    
    let routeDoc = null;
    if (route && String(route).match(/^[0-9a-fA-F]{24}$/)) {
      routeDoc = await Route.findById(route);
    }
    if (!routeDoc && route) {
      routeDoc = await Route.findOne({ $or: [{ routeNumber: route }, { name: route }] });
    }
    if (!routeDoc) return error(res, 'Route not found', 404);

    let busDoc = null;
    if (bus && String(bus).match(/^[0-9a-fA-F]{24}$/)) {
      busDoc = await Bus.findById(bus);
    }
    if (!busDoc && bus) {
      busDoc = await Bus.findOne({ $or: [{ busNumber: bus }, { name: bus }] });
    }
    if (!busDoc) return error(res, 'Bus not found', 404);

    let conductorId = null;
    if (conductor) {
      if (String(conductor).match(/^[0-9a-fA-F]{24}$/)) {
        const condUser = await User.findById(conductor);
        if (condUser) conductorId = condUser._id;
      } else {
        const condUser = await User.findOne({ $or: [{ mobile: conductor }, { name: conductor }] });
        if (condUser) conductorId = condUser._id;
      }
    }

    const schedule = await Schedule.create({
      route: routeDoc._id,
      bus: busDoc._id,
      departureTime,
      arrivalTime,
      daysOfOperation: daysOfOperation || ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
      fare: fare || routeDoc.baseFare || 300,
      conductor: conductorId || undefined,
    });
    
    const populated = await Schedule.findById(schedule._id)
      .populate('route')
      .populate('bus')
      .populate('conductor', 'name mobile photo role');
    return success(res, populated, 'Schedule created successfully', 201);
  } catch (err) {
    console.error('[ADD_SCHEDULE_ERROR]', err);
    return error(res, err.message || 'Failed to create schedule', 500);
  }
};

const getSchedules = async (req, res) => {
  try {
    const { routeId, busId, status } = req.query;
    const filter = {};
    if (routeId) filter.route = routeId;
    if (busId) filter.bus = busId;
    if (status) filter.status = status;
    
    const schedules = await Schedule.find(filter)
      .populate('route')
      .populate('bus')
      .populate('conductor', 'name mobile photo role')
      .sort({ departureTime: 1 });
    
    return success(res, schedules);
  } catch (err) {
    return error(res, 'Failed to fetch schedules', 500);
  }
};

const getSchedule = async (req, res) => {
  try {
    const schedule = await Schedule.findById(req.params.id)
      .populate('route')
      .populate('bus')
      .populate('conductor', 'name mobile photo role');
    if (!schedule) return error(res, 'Schedule not found', 404);
    return success(res, schedule);
  } catch (err) {
    return error(res, 'Failed to fetch schedule', 500);
  }
};

const updateSchedule = async (req, res) => {
  try {
    const updateData = { ...req.body };
    if (updateData.conductor === '') {
      updateData.conductor = null;
    }
    const schedule = await Schedule.findByIdAndUpdate(req.params.id, updateData, { new: true })
      .populate('route')
      .populate('bus')
      .populate('conductor', 'name mobile photo role');
    if (!schedule) return error(res, 'Schedule not found', 404);
    return success(res, schedule, 'Schedule updated successfully');
  } catch (err) {
    return error(res, 'Failed to update schedule', 500);
  }
};

const deleteSchedule = async (req, res) => {
  try {
    const schedule = await Schedule.findByIdAndDelete(req.params.id);
    if (!schedule) return error(res, 'Schedule not found', 404);
    return success(res, null, 'Schedule deleted successfully');
  } catch (err) {
    return error(res, 'Failed to delete schedule', 500);
  }
};

/**
 * Conductor: Get currently assigned bus schedule, today's manifest, and tracking state
 */
const getConductorDuty = async (req, res) => {
  try {
    const conductorId = req.user._id;

    // Find schedule assigned to this conductor
    let schedule = await Schedule.findOne({ conductor: conductorId, status: 'active' })
      .populate('route')
      .populate('bus')
      .populate('conductor', 'name mobile photo role');

    // Fallback for admin testing: find the first active schedule with a conductor or any active schedule
    if (!schedule && ['admin', 'owner', 'superadmin'].includes(req.user.role)) {
      schedule = await Schedule.findOne({ status: 'active' })
        .populate('route')
        .populate('bus')
        .populate('conductor', 'name mobile photo role');
    }

    if (!schedule) {
      return success(res, {
        schedule: null,
        passengers: [],
        activeTracking: null,
        message: 'No bus schedule currently assigned to you. Please ask Admin to assign a schedule.',
      });
    }

    // Get today's bookings for this schedule
    const todayStart = dayjs().startOf('day').toDate();
    const todayEnd = dayjs().endOf('day').toDate();

    const passengers = await Booking.find({
      schedule: schedule._id,
      journeyDate: { $gte: todayStart, $lte: todayEnd },
      status: { $in: ['CONFIRMED', 'PENDING_VERIFICATION'] },
    }).select('pnr referenceCode passengerName passengerMobile seats totalFare status paymentMethod confirmedAt');

    // Check if live tracking is currently active for this schedule
    const activeTracking = await BusTracking.findOne({
      schedule: schedule._id,
      isActive: true,
    });

    return success(res, {
      schedule,
      passengers,
      activeTracking: activeTracking ? {
        isActive: activeTracking.isActive,
        location: activeTracking.location,
        speed: activeTracking.speed,
        heading: activeTracking.heading,
        lastUpdated: activeTracking.lastUpdated,
      } : null,
    });
  } catch (err) {
    console.error('[CONDUCTOR_DUTY_ERROR]', err);
    return error(res, 'Failed to fetch conductor duty details', 500);
  }
};

/**
 * Search schedules — public endpoint for users
 * Find schedules for a specific route on a specific date
 */
const searchSchedules = async (req, res) => {
  try {
    const { source, destination, date } = req.query;
    
    if (!source || !destination) {
      return error(res, 'Source and destination are required', 400);
    }
    
    const escapeRegex = (str) => String(str).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const sourceRegex = new RegExp(escapeRegex(source.trim()), 'i');
    const destRegex = new RegExp(escapeRegex(destination.trim()), 'i');

    // Find routes where source & destination match termini or intermediate stops
    const candidateRoutes = await Route.find({
      status: 'active',
      $and: [
        { $or: [{ source: sourceRegex }, { 'stops.name': sourceRegex }] },
        { $or: [{ destination: destRegex }, { 'stops.name': destRegex }] },
      ],
    });

    // Ensure source stop occurs before destination stop in route direction
    const matchingRoutes = candidateRoutes.filter(r => {
      const allStops = [
        { name: r.source, order: -1 },
        ...(r.stops || []).map(s => ({ name: s.name, order: s.order })),
        { name: r.destination, order: 999999 },
      ].sort((a, b) => a.order - b.order);

      const firstSourceIdx = allStops.findIndex(s => sourceRegex.test(s.name));
      if (firstSourceIdx === -1) return false;

      return allStops.slice(firstSourceIdx + 1).some(s => destRegex.test(s.name));
    });
    
    if (matchingRoutes.length === 0) {
      return success(res, [], 'No routes found for this search');
    }
    
    const routeIds = matchingRoutes.map(r => r._id);
    
    // Build schedule filter
    const filter = {
      route: { $in: routeIds },
      status: 'active',
    };
    
    // Filter by day of week if date provided
    if (date) {
      const dayOfWeek = dayjs(date).format('ddd'); // Mon, Tue, etc.
      filter.$or = [
        { daysOfOperation: dayOfWeek },
        { daysOfOperation: { $size: 0 } },
        { daysOfOperation: { $exists: false } },
        { daysOfOperation: null }
      ];
    }
    
    const schedules = await Schedule.find(filter)
      .populate('route')
      .populate('bus')
      .populate('conductor', 'name mobile photo role')
      .sort({ departureTime: 1 });
    
    return success(res, schedules);
  } catch (err) {
    console.error('[SCHEDULE] searchSchedules error:', err);
    return error(res, 'Failed to search schedules', 500);
  }
};

module.exports = {
  addSchedule,
  getSchedules,
  getSchedule,
  updateSchedule,
  deleteSchedule,
  getConductorDuty,
  searchSchedules,
};
