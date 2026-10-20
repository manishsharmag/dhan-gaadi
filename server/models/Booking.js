const mongoose = require('mongoose');
const { v4: uuidv4 } = require('uuid');

// Generate PNR: 10-char alphanumeric
function generatePNR() {
  return 'HR' + uuidv4().replace(/-/g, '').substring(0, 8).toUpperCase();
}

// Generate reference code: HRB-XXXXXX
function generateRefCode() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // No confusing chars (0,O,1,I)
  let code = '';
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return 'HRB-' + code;
}

const bookingSchema = new mongoose.Schema({
  pnr: { type: String, unique: true, default: generatePNR },
  referenceCode: { type: String, unique: true, default: generateRefCode },
  
  user: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  schedule: { type: mongoose.Schema.Types.ObjectId, ref: 'Schedule', required: true },
  journeyDate: { type: Date, required: true },
  
  seats: [{ type: Number, required: true }], // seat numbers array
  
  // Passenger details
  passengerName: { type: String, required: true },
  passengerMobile: { type: String, required: true },
  passengerAge: { type: Number },
  passengerGender: { type: String, enum: ['male', 'female', 'other'] },
  
  // Boarding and Dropping points
  boardingPoint: { type: String, default: '' },
  droppingPoint: { type: String, default: '' },
  
  // Multi-passenger details (for multi-seat bookings)
  passengers: [{
    seatNumber: { type: Number, required: true },
    name: { type: String, required: true },
    age: { type: Number },
    gender: { type: String, enum: ['male', 'female', 'other'] }
  }],
  
  totalFare: { type: Number, required: true },
  
  status: { 
    type: String, 
    enum: ['SEAT_LOCKED', 'PENDING_VERIFICATION', 'CONFIRMED', 'REJECTED', 'EXPIRED', 'CANCELLED'],
    default: 'SEAT_LOCKED'
  },
  
  paymentScreenshot: { type: String, default: '' }, // file path
  seatLockExpiry: { type: Date }, // when the 10-min lock expires
  
  paymentMethod: { 
    type: String, 
    enum: ['online', 'cash', 'upi_direct'], 
    default: 'online' 
  },
  bookedBy: { type: mongoose.Schema.Types.ObjectId, ref: 'User' }, // Tracks if conductor made the booking
  
  confirmedAt: { type: Date },
  rejectedAt: { type: Date },
  rejectionReason: { type: String, default: '' },
  
  // Ticket
  ticketUrl: { type: String, default: '' },
}, { timestamps: true });

// Index for quick lookups
bookingSchema.index({ schedule: 1, journeyDate: 1, status: 1 });
bookingSchema.index({ status: 1, journeyDate: 1 });
bookingSchema.index({ user: 1 });
bookingSchema.index({ pnr: 1 });
bookingSchema.index({ referenceCode: 1 });

module.exports = mongoose.model('Booking', bookingSchema);
