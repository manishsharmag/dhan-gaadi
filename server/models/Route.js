const mongoose = require('mongoose');

const routeSchema = new mongoose.Schema({
  routeNumber: { 
    type: String, 
    required: true, 
    unique: true
  },
  name: { type: String, required: true }, // e.g., "Hisar - Chandigarh"
  source: { type: String, required: true },
  destination: { type: String, required: true },
  sourceDepot: { type: String, required: true },
  destinationDepot: { type: String, default: '' },
  stops: [{
    name: { type: String, required: true },
    distanceFromSource: { type: Number, default: 0 }, // in km
    order: { type: Number, required: true },
    lat: { type: Number, default: 0 },
    lng: { type: Number, default: 0 },
  }],
  totalDistance: { type: Number, required: true }, // in km
  baseFare: { type: Number, required: true }, // in rupees
  images: [{ type: String }], // Banner / route showcase images
  status: { 
    type: String, 
    enum: ['active', 'inactive'],
    default: 'active' 
  },
}, { timestamps: true });

module.exports = mongoose.model('Route', routeSchema);
