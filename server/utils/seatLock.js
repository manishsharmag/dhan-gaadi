const SeatLock = require('../models/SeatLock');
const Booking = require('../models/Booking');
const dayjs = require('dayjs');

const LOCK_DURATION_MINUTES = 10;

/**
 * Lock seats for a booking. Returns locked seats or throws if any seat is already locked.
 */
async function lockSeats(scheduleId, journeyDate, seatNumbers, bookingId, userId) {
  const dateStr = dayjs(journeyDate).format('YYYY-MM-DD');
  const now = new Date();
  const expiresAt = new Date(Date.now() + LOCK_DURATION_MINUTES * 60 * 1000);
  
  // 1. Check if any seats are already actively locked by another user/booking
  const existingLocks = await SeatLock.find({
    scheduleId,
    journeyDate: dateStr,
    seatNumber: { $in: seatNumbers },
    expiresAt: { $gt: now },
    ...(bookingId ? { bookingId: { $ne: bookingId } } : {}),
  });
  
  if (existingLocks.length > 0) {
    const lockedSeatNums = existingLocks.map(l => l.seatNumber);
    throw new Error(`Seats ${lockedSeatNums.join(', ')} are currently locked by another passenger`);
  }
  
  // 2. Check if any seats are already confirmed or pending verification in bookings
  const bookingFilter = {
    schedule: scheduleId,
    journeyDate: { 
      $gte: dayjs(journeyDate).startOf('day').toDate(),
      $lte: dayjs(journeyDate).endOf('day').toDate()
    },
    seats: { $in: seatNumbers },
    $or: [
      { status: { $in: ['CONFIRMED', 'PENDING_VERIFICATION'] } },
      { status: 'SEAT_LOCKED', seatLockExpiry: { $gt: now } }
    ]
  };
  
  if (bookingId) {
    bookingFilter._id = { $ne: bookingId };
  }
  
  const collidingBookings = await Booking.find(bookingFilter);
  
  if (collidingBookings.length > 0) {
    const bookedSeats = [...new Set(collidingBookings.flatMap(b => b.seats.filter(s => seatNumbers.includes(s))))];
    throw new Error(`Seats ${bookedSeats.join(', ')} are already booked or being processed`);
  }
  
  // 3. Clean any expired locks for these seats before creating new ones
  await SeatLock.deleteMany({
    scheduleId,
    journeyDate: dateStr,
    seatNumber: { $in: seatNumbers },
    expiresAt: { $lte: now }
  });
  
  // 4. Create locks
  const locks = seatNumbers.map(seatNumber => ({
    scheduleId,
    journeyDate: dateStr,
    seatNumber,
    bookingId,
    userId,
    expiresAt,
  }));
  
  try {
    await SeatLock.insertMany(locks);
  } catch (err) {
    // Duplicate key error means race condition — another user locked first
    if (err.code === 11000) {
      throw new Error('Some seats were just locked by another user. Please try again.');
    }
    throw err;
  }
  
  return { expiresAt, lockDurationMinutes: LOCK_DURATION_MINUTES };
}

/**
 * Release seats for a booking
 */
async function releaseSeats(scheduleId, journeyDate, seatNumbers) {
  const dateStr = dayjs(journeyDate).format('YYYY-MM-DD');
  await SeatLock.deleteMany({
    scheduleId,
    journeyDate: dateStr,
    seatNumber: { $in: seatNumbers },
  });
}

/**
 * Release all seats for a booking by bookingId
 */
async function releaseSeatsByBooking(bookingId) {
  await SeatLock.deleteMany({ bookingId });
}

/**
 * Get all locked/booked seats for a schedule on a date
 */
async function getUnavailableSeats(scheduleId, journeyDate) {
  const dateStr = dayjs(journeyDate).format('YYYY-MM-DD');
  const now = new Date();
  
  // 1. Get temporary active locks
  const locks = await SeatLock.find({
    scheduleId,
    journeyDate: dateStr,
    expiresAt: { $gt: now },
  }).lean();
  
  // 2. Get confirmed and pending bookings (and active seat locked bookings)
  const activeBookings = await Booking.find({
    schedule: scheduleId,
    journeyDate: {
      $gte: dayjs(journeyDate).startOf('day').toDate(),
      $lte: dayjs(journeyDate).endOf('day').toDate(),
    },
    $or: [
      { status: { $in: ['CONFIRMED', 'PENDING_VERIFICATION'] } },
      { status: 'SEAT_LOCKED', seatLockExpiry: { $gt: now } }
    ]
  }).lean();
  
  const lockedSeatNums = new Set(locks.map(l => l.seatNumber));
  const bookedSeatNums = new Set(activeBookings.flatMap(b => b.seats));
  
  const lockedSeats = locks.map(l => ({ seatNumber: l.seatNumber, status: 'locked', expiresAt: l.expiresAt }));
  const bookedSeats = Array.from(bookedSeatNums)
    .filter(seatNum => !lockedSeatNums.has(seatNum))
    .map(seatNumber => ({ seatNumber, status: 'booked' }));
  
  return [...lockedSeats, ...bookedSeats];
}

module.exports = { lockSeats, releaseSeats, releaseSeatsByBooking, getUnavailableSeats, LOCK_DURATION_MINUTES };
