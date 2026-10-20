const mongoose = require('mongoose');

const qrCodeSchema = new mongoose.Schema({
  imagePath: { type: String, required: true }, // path to uploaded QR image
  upiId: { type: String, default: '' },
  upiLabel: { type: String, required: true }, // e.g., "Dhan-Gaadi Fleet UPI"
  isActive: { type: Boolean, default: false },
  uploadedBy: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
}, { timestamps: true });

module.exports = mongoose.model('QRCode', qrCodeSchema);
