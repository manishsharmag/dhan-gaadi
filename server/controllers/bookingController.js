const path = require('path');
const fs = require('fs');
const mongoose = require('mongoose');
const Booking = require('../models/Booking');
const Schedule = require('../models/Schedule');
const Route = require('../models/Route');
const Bus = require('../models/Bus');
const { lockSeats, releaseSeats, releaseSeatsByBooking, getUnavailableSeats } = require('../utils/seatLock');
const { generateTicket } = require('../services/ticketService');
const smsService = require('../services/smsService');
const { success, error } = require('../utils/response');
const dayjs = require('dayjs');

/**
 * Get unavailable seats for a schedule on a date
 */
const getSeatAvailability = async (req, res) => {
  try {
    const { scheduleId, date } = req.query;

    if (!scheduleId || !date) {
      return error(res, 'Schedule ID and date are required', 400);
    }

    const schedule = await Schedule.findById(scheduleId).populate('bus');
    if (!schedule) {
      return error(res, 'Schedule not found', 404);
    }

    const unavailable = await getUnavailableSeats(scheduleId, date);

    return success(res, {
      totalSeats: schedule.bus.totalSeats,
      unavailableSeats: unavailable,
    });
  } catch (err) {
    console.error('[BOOKING] getSeatAvailability error:', err);
    return error(res, 'Failed to fetch seat availability', 500);
  }
};

/**
 * Create booking — locks seats for 10 minutes
 */
const createBooking = async (req, res) => {
  try {
    const {
      scheduleId,
      journeyDate,
      seats,
      passengerName,
      passengerMobile,
      passengerAge,
      passengerGender,
      boardingPoint,
      droppingPoint,
      passengers,
    } = req.body;
    const requestedPaymentMethod = String(req.body.paymentMethod || 'online').toLowerCase();
    const paymentMethod = requestedPaymentMethod === 'upi' ? 'upi_direct' : requestedPaymentMethod;
    const allowedPaymentMethods = ['online', 'cash', 'upi_direct'];
    if (!allowedPaymentMethods.includes(paymentMethod)) {
      return error(res, 'Invalid payment method', 400);
    }

    const staffRoles = ['admin', 'owner', 'superadmin', 'conductor'];
    const isStaffDirectPayment = staffRoles.includes(req.user.role) && ['cash', 'upi_direct'].includes(paymentMethod);

    // Validate passenger inputs
    if (!passengerName || !passengerName.trim()) {
      return error(res, 'Passenger name is required', 400);
    }
    const cleanMobile = String(passengerMobile || '').replace(/\D/g, '').slice(-10);
    if (!/^[6-9]\d{9}$/.test(cleanMobile)) {
      return error(res, 'Please provide a valid 10-digit Indian mobile number', 400);
    }

    // Validate seats array
    if (!Array.isArray(seats) || seats.length === 0) {
      return error(res, 'Please select at least one seat', 400);
    }

    // Validate schedule exists
    const schedule = await Schedule.findById(scheduleId).populate('route').populate('bus');
    if (!schedule) {
      return error(res, 'Schedule not found', 404);
    }

    if (!schedule.bus || !schedule.bus.totalSeats) {
      return error(res, 'Bus configuration unavailable for this schedule', 400);
    }

    // Validate seats are within range
    const totalSeats = schedule.bus.totalSeats;
    for (const seat of seats) {
      if (seat < 1 || seat > totalSeats) {
        return error(res, `Seat ${seat} is invalid. Must be between 1 and ${totalSeats}`, 400);
      }
    }

    // Calculate fare
    const fare = schedule.fare || schedule.route.baseFare;
    const totalFare = fare * seats.length;

    // Format passengers array if provided
    let formattedPassengers = undefined;
    if (Array.isArray(passengers) && passengers.length > 0) {
      formattedPassengers = passengers.map((p) => ({
        seatNumber: Number(p.seatNumber),
        name: String(p.name || '').trim(),
        age: p.age ? Number(p.age) : undefined,
        gender: p.gender || undefined,
      }));
    }

    // Create booking first
    const booking = await Booking.create({
      user: req.user._id,
      schedule: scheduleId,
      journeyDate: new Date(journeyDate),
      seats,
      passengerName: passengerName.trim(),
      passengerMobile: cleanMobile,
      passengerAge,
      passengerGender,
      boardingPoint: boardingPoint || schedule.route?.source || '',
      droppingPoint: droppingPoint || schedule.route?.destination || '',
      passengers: formattedPassengers,
      totalFare,
      paymentMethod,
      bookedBy: isStaffDirectPayment ? req.user._id : undefined,
      status: isStaffDirectPayment ? 'CONFIRMED' : 'SEAT_LOCKED',
      confirmedAt: isStaffDirectPayment ? new Date() : undefined,
      seatLockExpiry: isStaffDirectPayment ? undefined : new Date(Date.now() + 10 * 60 * 1000),
    });

    // Lock seats
    try {
      const lockResult = await lockSeats(scheduleId, journeyDate, seats, booking._id, req.user._id);

      if (isStaffDirectPayment) {
        await releaseSeatsByBooking(booking._id);
        try {
          await generateTicket(booking, schedule);
          booking.ticketUrl = `/tickets/${booking.pnr}.pdf`;
          await booking.save();
        } catch (ticketErr) {
          console.error('[TICKET] POS generation error:', ticketErr);
        }
      }

      return success(res, {
        booking: {
          id: booking._id,
          pnr: booking.pnr,
          referenceCode: booking.referenceCode,
          seats: booking.seats,
          totalFare: booking.totalFare,
          status: booking.status,
          seatLockExpiry: booking.seatLockExpiry,
        },
        lockExpiresAt: lockResult.expiresAt,
      }, isStaffDirectPayment ? 'Walk-in booking confirmed successfully.' : 'Seats locked successfully. Complete payment within 10 minutes.', 201);
    } catch (lockErr) {
      // If seat locking fails, delete the booking
      await Booking.findByIdAndDelete(booking._id);
      return error(res, lockErr.message, 409);
    }
  } catch (err) {
    console.error('[BOOKING] createBooking error:', err);
    return error(res, 'Failed to create booking', 500);
  }
};

/**
 * Instant Demo Payment — skips external gateway and admin approval for investor demos/testing
 * Immediately marks booking CONFIRMED, generates ticket PDF, releases seat locks, and returns ticket URL
 */
const instantDemoPayment = async (req, res) => {
  try {
    const { bookingId } = req.params;

    const booking = await Booking.findById(bookingId).populate('user', 'name mobile');
    if (!booking) {
      return error(res, 'Booking not found', 404);
    }

    // Check ownership
    const isOwner = req.user && (
      String(booking.user?._id || booking.user) === String(req.user._id) ||
      String(booking.bookedBy?._id || booking.bookedBy) === String(req.user._id)
    );
    const isStaff = req.user && ['admin', 'owner', 'superadmin', 'conductor'].includes(req.user.role);
    if (!isOwner && !isStaff) {
      return error(res, 'Not authorized to modify this booking', 403);
    }

    if (booking.status === 'CONFIRMED') {
      return success(res, {
        bookingId: booking._id,
        pnr: booking.pnr,
        status: booking.status,
        ticketUrl: booking.ticketUrl || `/tickets/${booking.pnr}.pdf`,
      }, 'Booking is already confirmed.');
    }

    if (booking.status === 'CANCELLED' || booking.status === 'REJECTED') {
      return error(res, `Cannot complete payment for ${booking.status.toLowerCase()} booking`, 400);
    }

    const schedule = await Schedule.findById(booking.schedule)
      .populate('route')
      .populate('bus');

    // Update booking to CONFIRMED immediately
    booking.status = 'CONFIRMED';
    booking.paymentMethod = 'upi_direct';
    booking.confirmedAt = new Date();

    // Generate PDF ticket
    try {
      await generateTicket(booking, schedule);
      booking.ticketUrl = `/tickets/${booking.pnr}.pdf`;
    } catch (ticketErr) {
      console.error('[TICKET] Instant demo generation error:', ticketErr);
    }

    await booking.save();

    // Release seat locks (seats are permanently booked)
    await releaseSeatsByBooking(booking._id);

    // Primary frontend URL for ticket link
    const primaryFrontend = (process.env.FRONTEND_URL || 'https://haryanabusinfo.in')
      .split(',')[0]
      .trim()
      .replace(/\/+$/, '');

    // Send SMS confirmation (logged in dev / Fast2SMS in prod)
    try {
      await smsService.sendBookingConfirmation(booking.passengerMobile, {
        referenceCode: booking.referenceCode,
        pnr: booking.pnr,
        seats: booking.seats,
        ticketUrl: `${primaryFrontend}/ticket/${booking.pnr}`,
      });
    } catch (smsErr) {
      console.warn('[SMS] Instant demo SMS notification error:', smsErr.message);
    }

    return success(res, {
      bookingId: booking._id,
      pnr: booking.pnr,
      status: booking.status,
      ticketUrl: booking.ticketUrl,
    }, 'Payment verified instantly via Dhan-Gaadi Instant Pay (Demo Mode).');
  } catch (err) {
    console.error('[BOOKING] instantDemoPayment error:', err);
    return error(res, 'Failed to complete instant demo payment', 500);
  }
};

/**
 * Submit payment — moves from SEAT_LOCKED to PENDING_VERIFICATION (or CONFIRMED if demo mode)
 */
const submitPayment = async (req, res) => {
  try {
    const { bookingId } = req.params;

    // Check for demo bypass flag
    const isInstantDemo = req.body?.instantDemo === true || req.body?.instantDemo === 'true' || req.query?.demo === 'true';
    if (isInstantDemo) {
      return instantDemoPayment(req, res);
    }

    const booking = await Booking.findById(bookingId);
    if (!booking) {
      return error(res, 'Booking not found', 404);
    }

    // Check ownership
    if (booking.user && req.user && booking.user.toString() !== req.user._id.toString() && !['admin', 'owner', 'superadmin', 'conductor'].includes(req.user.role)) {
      return error(res, 'Not authorized to modify this booking', 403);
    }

    // Check status
    if (booking.status !== 'SEAT_LOCKED' && booking.status !== 'PENDING_VERIFICATION') {
      return error(res, `Cannot submit payment for booking in ${booking.status} status`, 400);
    }

    // Check if lock expired (only if SEAT_LOCKED)
    if (booking.status === 'SEAT_LOCKED' && booking.seatLockExpiry && booking.seatLockExpiry < new Date()) {
      booking.status = 'EXPIRED';
      await booking.save();
      await releaseSeatsByBooking(booking._id);
      return error(res, 'Seat lock has expired. Please book again.', 410);
    }

    // Handle screenshot upload (if present)
    if (req.file) {
      booking.paymentScreenshot = `/img/${req.file.filename}`;
    }

    booking.status = 'PENDING_VERIFICATION';
    await booking.save();

    return success(res, {
      bookingId: booking._id,
      referenceCode: booking.referenceCode,
      status: booking.status,
    }, 'Payment submitted. Awaiting admin verification.');
  } catch (err) {
    console.error('[BOOKING] submitPayment error:', err);
    return error(res, 'Failed to submit payment', 500);
  }
};

/**
 * Admin: Get pending verification queue
 */
const getPendingBookings = async (req, res) => {
  try {
    const bookings = await Booking.find({ status: 'PENDING_VERIFICATION' })
      .populate('user', 'name mobile')
      .populate({
        path: 'schedule',
        populate: [
          { path: 'route', select: 'routeNumber name source destination' },
          { path: 'bus', select: 'busNumber type' },
          { path: 'conductor', select: 'name mobile photo role' }
        ]
      })
      .sort({ createdAt: -1 });

    return success(res, bookings);
  } catch (err) {
    console.error('[BOOKING] getPendingBookings error:', err);
    return error(res, 'Failed to fetch pending bookings', 500);
  }
};

/**
 * Admin: Confirm a booking
 */
const confirmBooking = async (req, res) => {
  try {
    const { bookingId } = req.params;

    const booking = await Booking.findById(bookingId)
      .populate('user', 'name mobile');
    if (!booking) {
      return error(res, 'Booking not found', 404);
    }

    if (booking.status !== 'PENDING_VERIFICATION') {
      return error(res, `Cannot confirm booking in ${booking.status} status`, 400);
    }

    // Get schedule with populated route and bus for ticket
    const schedule = await Schedule.findById(booking.schedule)
      .populate('route')
      .populate('bus');

    // Update booking status
    booking.status = 'CONFIRMED';
    booking.confirmedAt = new Date();

    // Generate PDF ticket
    try {
      const ticketPath = await generateTicket(booking, schedule);
      booking.ticketUrl = `/tickets/${booking.pnr}.pdf`;
    } catch (ticketErr) {
      console.error('[TICKET] Generation error:', ticketErr);
      // Don't fail confirmation if ticket generation fails
    }

    await booking.save();

    // Release seat locks (seats are now permanently booked)
    await releaseSeatsByBooking(booking._id);

    // Extract primary frontend origin for clean SMS links
    const primaryFrontend = (process.env.FRONTEND_URL || 'https://haryanabusinfo.in')
      .split(',')[0]
      .trim()
      .replace(/\/+$/, '');

    // Send SMS confirmation
    await smsService.sendBookingConfirmation(booking.passengerMobile, {
      referenceCode: booking.referenceCode,
      pnr: booking.pnr,
      seats: booking.seats,
      ticketUrl: `${primaryFrontend}/ticket/${booking.pnr}`,
    });

    return success(res, {
      bookingId: booking._id,
      pnr: booking.pnr,
      status: booking.status,
      ticketUrl: booking.ticketUrl,
    }, 'Booking confirmed successfully');
  } catch (err) {
    console.error('[BOOKING] confirmBooking error:', err);
    return error(res, 'Failed to confirm booking', 500);
  }
};

/**
 * Admin: Reject a booking
 */
const rejectBooking = async (req, res) => {
  try {
    const { bookingId } = req.params;
    const { reason } = req.body;

    const booking = await Booking.findById(bookingId);
    if (!booking) {
      return error(res, 'Booking not found', 404);
    }

    if (booking.status !== 'PENDING_VERIFICATION') {
      return error(res, `Cannot reject booking in ${booking.status} status`, 400);
    }

    booking.status = 'REJECTED';
    booking.rejectedAt = new Date();
    booking.rejectionReason = (reason || 'Payment screenshot could not be verified or invalid amount').trim();
    await booking.save();

    // Release seats immediately
    await releaseSeatsByBooking(booking._id);

    // Send SMS
    await smsService.sendBookingRejection(booking.passengerMobile, {
      referenceCode: booking.referenceCode,
      reason: booking.rejectionReason,
    });

    return success(res, {
      bookingId: booking._id,
      status: booking.status,
      rejectionReason: booking.rejectionReason,
    }, 'Booking rejected. Seats released.');
  } catch (err) {
    console.error('[BOOKING] rejectBooking error:', err);
    return error(res, 'Failed to reject booking', 500);
  }
};

/**
 * User / Staff: Cancel a booking
 */
const cancelBooking = async (req, res) => {
  try {
    const { bookingId } = req.params;
    const booking = await Booking.findById(bookingId);
    if (!booking) {
      return error(res, 'Booking not found', 404);
    }

    // Ensure only the booking owner, booking creator, or staff can cancel
    const isOwner = req.user && (
      String(booking.user?._id || booking.user) === String(req.user._id) ||
      String(booking.bookedBy?._id || booking.bookedBy) === String(req.user._id)
    );
    const isStaff = req.user && ['admin', 'owner', 'superadmin', 'conductor'].includes(req.user.role);
    if (!isOwner && !isStaff) {
      return error(res, 'Not authorized to cancel this booking', 403);
    }

    if (['REJECTED', 'EXPIRED', 'CANCELLED'].includes(booking.status)) {
      return error(res, `Booking is already ${booking.status.toLowerCase()}`, 400);
    }

    booking.status = 'CANCELLED';
    await booking.save();

    // Release any active seat locks
    await releaseSeatsByBooking(booking._id);

    return success(res, {
      bookingId: booking._id,
      pnr: booking.pnr,
      status: booking.status,
    }, 'Booking cancelled successfully');
  } catch (err) {
    console.error('[BOOKING] cancelBooking error:', err);
    return error(res, 'Failed to cancel booking', 500);
  }
};

/**
 * Get all bookings (admin — with filters)
 */
const getAllBookings = async (req, res) => {
  try {
    const { status, date, routeId, page = 1, limit = 20 } = req.query;

    const filter = {};
    if (status) filter.status = status;
    if (routeId) filter.schedule = routeId; // Will need schedule lookup
    if (date) {
      filter.journeyDate = {
        $gte: dayjs(date).startOf('day').toDate(),
        $lte: dayjs(date).endOf('day').toDate(),
      };
    }

    const skip = (parseInt(page) - 1) * parseInt(limit);

    const [bookings, total] = await Promise.all([
      Booking.find(filter)
        .populate('user', 'name mobile')
        .populate({
          path: 'schedule',
          populate: [
            { path: 'route', select: 'routeNumber name source destination' },
            { path: 'bus', select: 'busNumber type' }
          ]
        })
        .sort({ createdAt: -1 })
        .skip(skip)
        .limit(parseInt(limit)),
      Booking.countDocuments(filter)
    ]);

    return success(res, {
      bookings,
      pagination: {
        total,
        page: parseInt(page),
        pages: Math.ceil(total / parseInt(limit)),
      }
    });
  } catch (err) {
    console.error('[BOOKING] getAllBookings error:', err);
    return error(res, 'Failed to fetch bookings', 500);
  }
};

/**
 * Get user's own bookings (matched by user account, creator, or passenger mobile)
 */
const getUserBookings = async (req, res) => {
  try {
    const userMobile = req.user.mobile || '';
    const cleanMobile = userMobile.replace(/\D/g, '').slice(-10);

    const conditions = [
      { user: req.user._id },
      { bookedBy: req.user._id },
    ];

    if (cleanMobile) {
      conditions.push({ passengerMobile: cleanMobile });
      conditions.push({ passengerMobile: `+91${cleanMobile}` });
      conditions.push({ passengerMobile: userMobile });
      // Regex search for last 10 digits in passengerMobile
      conditions.push({ passengerMobile: new RegExp(cleanMobile + '$') });
    }

    const bookings = await Booking.find({ $or: conditions })
      .populate('user', 'name mobile email role')
      .populate('bookedBy', 'name mobile email role')
      .populate({
        path: 'schedule',
        populate: [
          { path: 'route', select: 'routeNumber name source destination stops totalDistance baseFare' },
          { path: 'bus', select: 'busNumber type totalSeats' },
          { path: 'conductor', select: 'name mobile photo role' }
        ]
      })
      .sort({ createdAt: -1 });

    return success(res, bookings);
  } catch (err) {
    console.error('[BOOKING] getUserBookings error:', err);
    return error(res, 'Failed to fetch your bookings', 500);
  }
};

/**
 * Get booking by ID
 */
const getBooking = async (req, res) => {
  try {
    const booking = await Booking.findById(req.params.id)
      .populate('user', 'name mobile')
      .populate({
        path: 'schedule',
        populate: [
          { path: 'route' },
          { path: 'bus' },
          { path: 'conductor', select: 'name mobile photo role' }
        ]
      });

    if (!booking) {
      return error(res, 'Booking not found', 404);
    }

    // Check authorization: only booking owner or staff can view full details
    const isOwner = req.user && (
      String(booking.user?._id || booking.user) === String(req.user._id) ||
      String(booking.bookedBy) === String(req.user._id)
    );
    const isStaff = req.user && ['admin', 'owner', 'superadmin', 'conductor'].includes(req.user.role);
    if (!isOwner && !isStaff) {
      return error(res, 'Not authorized to view this booking', 403);
    }

    return success(res, booking);
  } catch (err) {
    console.error('[BOOKING] getBooking error:', err);
    return error(res, 'Failed to fetch booking', 500);
  }
};

/**
 * Get booking by PNR, Reference Code, or ID (public — for ticket page)
 */
const getBookingByPNR = async (req, res) => {
  try {
    const rawParam = decodeURIComponent(String(req.params.pnr || '')).trim();
    if (!rawParam) {
      return error(res, 'Booking identifier is required', 400);
    }

    const upperParam = rawParam.toUpperCase();
    const cleanParam = upperParam.replace(/[^A-Z0-9]/g, '');

    const conditions = [
      { pnr: upperParam },
      { pnr: cleanParam },
      { referenceCode: upperParam },
      { referenceCode: cleanParam }
    ];

    // Support reference codes without hyphens or prefix (e.g. HRBXXXXXX or XXXXXX)
    if (cleanParam.startsWith('HRB')) {
      const codePart = cleanParam.slice(3);
      if (codePart) {
        conditions.push({ referenceCode: `HRB-${codePart}` });
      }
    } else if (cleanParam.length === 6) {
      conditions.push({ referenceCode: `HRB-${cleanParam}` });
    }

    // Case-insensitive regex matching for robustness
    if (cleanParam) {
      conditions.push({ pnr: new RegExp(`^${cleanParam}$`, 'i') });
      const bareRef = cleanParam.replace(/^HRB/i, '');
      if (bareRef) {
        conditions.push({ referenceCode: new RegExp(`^HRB-?${bareRef}$`, 'i') });
      }
    }

    // Direct MongoDB ObjectId matching if valid
    if (mongoose.Types.ObjectId.isValid(rawParam)) {
      conditions.push({ _id: rawParam });
    }

    const booking = await Booking.findOne({ $or: conditions })
      .populate({
        path: 'schedule',
        populate: [
          { path: 'route', select: 'routeNumber name source destination totalDistance stops' },
          { path: 'bus', select: 'busNumber type depot' },
          { path: 'conductor', select: 'name mobile photo role' }
        ]
      });

    if (!booking) {
      return error(res, 'Booking not found. Please verify your PNR or Reference Code.', 404);
    }

    // Mask passenger mobile for public access to protect PII
    const rawMobile = String(booking.passengerMobile || '').trim();
    const maskedMobile = rawMobile.length >= 10
      ? 'XXXXXX' + rawMobile.slice(-4)
      : rawMobile.replace(/.(?=.{2})/g, 'X');

    // Only return limited info for public access
    return success(res, {
      _id: booking._id,
      pnr: booking.pnr,
      referenceCode: booking.referenceCode,
      passengerName: booking.passengerName,
      passengerMobile: maskedMobile,
      seats: booking.seats,
      totalFare: booking.totalFare,
      status: booking.status,
      journeyDate: booking.journeyDate,
      rejectionReason: booking.rejectionReason || '',
      ticketUrl: booking.ticketUrl || `/tickets/${booking.pnr}.pdf`,
      schedule: booking.schedule,
    });
  } catch (err) {
    console.error('[BOOKING] getBookingByPNR error:', err);
    return error(res, 'Failed to fetch booking', 500);
  }
};

/**
 * On-demand ticket PDF download / stream handler
 * Generates PDF on-the-fly if missing from disk
 */
const downloadTicketPDF = async (req, res) => {
  try {
    const rawParam = decodeURIComponent(String(req.params.pnr || '')).trim();
    if (!rawParam) {
      return res.status(400).send('Booking identifier is required');
    }

    const baseName = rawParam.replace(/\.pdf$/i, '').trim();
    const upperParam = baseName.toUpperCase();
    const cleanParam = upperParam.replace(/[^A-Z0-9]/g, '');

    const ticketsDir = path.join(__dirname, '..', 'public', 'tickets');
    if (!fs.existsSync(ticketsDir)) {
      fs.mkdirSync(ticketsDir, { recursive: true });
    }

    // 1. Check if the PDF file already exists on disk
    const candidatePaths = [
      path.join(ticketsDir, `${baseName}.pdf`),
      path.join(ticketsDir, `${cleanParam}.pdf`),
      path.join(ticketsDir, `${upperParam}.pdf`),
      path.join(ticketsDir, `${rawParam}`),
    ];

    for (const cp of candidatePaths) {
      if (fs.existsSync(cp) && fs.statSync(cp).isFile()) {
        res.setHeader('Content-Type', 'application/pdf');
        res.setHeader('Content-Disposition', `inline; filename="${path.basename(cp)}"`);
        return res.sendFile(path.resolve(cp));
      }
    }

    // 2. Query booking from MongoDB if PDF not yet written to disk
    const conditions = [
      { pnr: upperParam },
      { pnr: cleanParam },
      { referenceCode: upperParam },
      { referenceCode: cleanParam }
    ];

    if (cleanParam.startsWith('HRB')) {
      const codePart = cleanParam.slice(3);
      if (codePart) conditions.push({ referenceCode: `HRB-${codePart}` });
    } else if (cleanParam.length === 6) {
      conditions.push({ referenceCode: `HRB-${cleanParam}` });
    }

    if (cleanParam) {
      conditions.push({ pnr: new RegExp(`^${cleanParam}$`, 'i') });
      const bareRef = cleanParam.replace(/^HRB/i, '');
      if (bareRef) conditions.push({ referenceCode: new RegExp(`^HRB-?${bareRef}$`, 'i') });
    }

    if (mongoose.Types.ObjectId.isValid(baseName)) {
      conditions.push({ _id: baseName });
    }

    const booking = await Booking.findOne({ $or: conditions })
      .populate('user', 'name mobile');

    if (!booking) {
      res.status(404);
      return res.send(`
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Ticket Not Found - Dhan-Gaadi Fleet</title>
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f8fafc; color: #1e293b; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; }
            .card { background: #ffffff; padding: 2.5rem; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); max-width: 440px; text-align: center; border: 1px solid #e2e8f0; }
            .icon { font-size: 3rem; margin-bottom: 1rem; }
            h2 { margin: 0 0 0.5rem; color: #0f172a; font-size: 1.5rem; }
            p { color: #64748b; margin-bottom: 1.5rem; font-size: 0.95rem; }
            .btn { display: inline-block; background: #1a365d; color: #ffffff; text-decoration: none; padding: 0.75rem 1.5rem; border-radius: 8px; font-weight: 600; font-size: 0.9rem; }
            .btn:hover { background: #2d3748; }
          </style>
        </head>
        <body>
          <div class="card">
            <div class="icon">🎫</div>
            <h2>Ticket Not Found</h2>
            <p>No booking was found matching identifier "<strong>${rawParam}</strong>". Please check your PNR or Reference Code.</p>
            <a href="/" class="btn">Return to Portal</a>
          </div>
        </body>
        </html>
      `);
    }

    // 3. Populate schedule and generate PDF ticket on-the-fly
    const schedule = await Schedule.findById(booking.schedule)
      .populate('route')
      .populate('bus');

    const generatedPath = await generateTicket(booking, schedule || {});
    if (!booking.ticketUrl) {
      booking.ticketUrl = `/tickets/${booking.pnr}.pdf`;
      await booking.save();
    }

    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `inline; filename="${booking.pnr}.pdf"`);
    return res.sendFile(path.resolve(generatedPath));
  } catch (err) {
    console.error('[TICKET-DOWNLOAD] Error:', err);
    return res.status(500).send('Failed to generate or retrieve ticket PDF');
  }
};

/**
 * Admin: Dashboard stats
 */
const getDashboardStats = async (req, res) => {
  try {
    const [
      totalBookings,
      pendingCount,
      confirmedCount,
      totalRevenue,
      todayBookings,
      todayRevenue,
      activeBuses,
      activeRoutes,
      uniqueCustomers,
      paymentMethodStats,
    ] = await Promise.all([
      Booking.countDocuments(),
      Booking.countDocuments({ status: 'PENDING_VERIFICATION' }),
      Booking.countDocuments({ status: 'CONFIRMED' }),
      Booking.aggregate([
        { $match: { status: 'CONFIRMED' } },
        { $group: { _id: null, total: { $sum: '$totalFare' } } }
      ]),
      Booking.countDocuments({
        createdAt: {
          $gte: dayjs().startOf('day').toDate(),
          $lte: dayjs().endOf('day').toDate(),
        }
      }),
      Booking.aggregate([
        {
          $match: {
            status: 'CONFIRMED',
            createdAt: {
              $gte: dayjs().startOf('day').toDate(),
              $lte: dayjs().endOf('day').toDate(),
            },
          }
        },
        { $group: { _id: null, total: { $sum: '$totalFare' } } }
      ]),
      Bus.countDocuments({ status: 'active' }),
      Route.countDocuments({ status: 'active' }),
      Booking.distinct('passengerMobile').then((mobiles) => mobiles.filter(Boolean).length),
      Booking.aggregate([
        { $group: { _id: '$paymentMethod', count: { $sum: 1 } } }
      ]),
    ]);

    const paymentCounts = paymentMethodStats.reduce((acc, item) => {
      acc[item._id || 'unknown'] = item.count;
      return acc;
    }, {});

    return success(res, {
      totalBookings,
      pendingVerification: pendingCount,
      confirmedBookings: confirmedCount,
      totalRevenue: totalRevenue[0]?.total || 0,
      todayBookings,
      todayRevenue: todayRevenue[0]?.total || 0,
      activeBuses,
      activeRoutes,
      uniqueCustomers,
      onlinePayments: paymentCounts.online || 0,
      cashPayments: paymentCounts.cash || 0,
      upiPayments: paymentCounts.upi_direct || 0,
    });
  } catch (err) {
    console.error('[BOOKING] getDashboardStats error:', err);
    return error(res, 'Failed to fetch dashboard stats', 500);
  }
};

/**
 * Admin: Export bookings as CSV
 */
const exportBookingsCSV = async (req, res) => {
  try {
    const { status, startDate, endDate } = req.query;

    const filter = {};
    if (status) filter.status = status;
    if (startDate || endDate) {
      filter.journeyDate = {};
      if (startDate) filter.journeyDate.$gte = dayjs(startDate).startOf('day').toDate();
      if (endDate) filter.journeyDate.$lte = dayjs(endDate).endOf('day').toDate();
    }

    const bookings = await Booking.find(filter)
      .populate('user', 'name mobile')
      .populate({
        path: 'schedule',
        populate: [
          { path: 'route', select: 'routeNumber name source destination' },
          { path: 'bus', select: 'busNumber type' }
        ]
      })
      .sort({ createdAt: -1 });

    // Build CSV safely with quote escaping and formula injection protection
    const escapeCsvField = (val) => {
      if (val === null || val === undefined) return '""';
      let str = String(val);
      // Neutralize CSV formula injection if cell begins with =, +, -, @, \t, or \r
      if (/^[=+\-@\t\r]/.test(str)) {
        str = `'${str}`;
      }
      return `"${str.replace(/"/g, '""')}"`;
    };

    const headers = ['PNR', 'Reference Code', 'Passenger', 'Mobile', 'Route', 'From', 'To', 'Bus', 'Seats', 'Fare', 'Status', 'Journey Date', 'Booked At'].map(escapeCsvField).join(',');
    const rows = bookings.map(b => {
      const route = b.schedule?.route;
      const bus = b.schedule?.bus;
      return [
        b.pnr,
        b.referenceCode,
        b.passengerName,
        b.passengerMobile,
        route?.routeNumber || '',
        route?.source || '',
        route?.destination || '',
        bus?.busNumber || '',
        (b.seats || []).join(';'),
        b.totalFare,
        b.status,
        b.journeyDate ? dayjs(b.journeyDate).format('DD-MM-YYYY') : '',
        b.createdAt ? dayjs(b.createdAt).format('DD-MM-YYYY HH:mm') : '',
      ].map(escapeCsvField).join(',');
    }).join('\n');

    res.setHeader('Content-Type', 'text/csv; charset=utf-8');
    res.setHeader('Content-Disposition', 'attachment; filename=bookings.csv');
    return res.send(headers + '\n' + rows);
  } catch (err) {
    console.error('[BOOKING] exportCSV error:', err);
    return error(res, 'Failed to export bookings', 500);
  }
};

module.exports = {
  getSeatAvailability,
  createBooking,
  submitPayment,
  instantDemoPayment,
  getPendingBookings,
  confirmBooking,
  rejectBooking,
  cancelBooking,
  getAllBookings,
  getUserBookings,
  getBooking,
  getBookingByPNR,
  downloadTicketPDF,
  getDashboardStats,
  exportBookingsCSV,
};
