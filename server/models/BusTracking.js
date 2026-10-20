const mongoose = require('mongoose');

const busTrackingSchema = new mongoose.Schema({
  schedule: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Schedule',
    required: true,
    index: true,
  },
  conductor: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
  },
  location: {
    lat: { type: Number, required: true },
    lng: { type: Number, required: true },
  },
  speed: { type: Number, default: 0 },       // km/h
  heading: { type: Number, default: 0 },      // degrees
  accuracy: { type: Number, default: 0 },     // meters
  currentStop: { type: String, default: '' }, // name of nearest passed stop
  nextStop: { type: String, default: '' },    // name of upcoming stop
  stopsReached: [{
    name: { type: String },
    arrivedAt: { type: Date },
  }],
  isActive: { type: Boolean, default: true },
  startedAt: { type: Date, default: Date.now },
  lastUpdated: { type: Date, default: Date.now },
}, { timestamps: true });

// Auto-expire tracking data after 24 hours
busTrackingSchema.index({ lastUpdated: 1 }, { expireAfterSeconds: 86400 });

// Only one active tracking per schedule
busTrackingSchema.index({ schedule: 1, isActive: 1 }, { unique: true, partialFilterExpression: { isActive: true } });

module.exports = mongoose.model('BusTracking', busTrackingSchema);
