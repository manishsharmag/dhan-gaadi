const QRCode = require('../models/QRCode');
const { success, error } = require('../utils/response');

/**
 * Upload/create a new QR code for payments
 */
const createQRCode = async (req, res) => {
  try {
    if (!req.file) {
      return error(res, 'QR code image is required', 400);
    }
    
    const { upiId, upiLabel } = req.body || {};
    const label = (upiLabel && upiLabel.trim()) ? upiLabel.trim() : 'Dhan-Gaadi Merchant';
    
    const qrCode = await QRCode.create({
      imagePath: `/img/${req.file.filename}`,
      upiId: (upiId || '').trim(),
      upiLabel: label,
      isActive: false,
      uploadedBy: req.user ? req.user._id : null,
    });
    
    return success(res, qrCode, 'QR code uploaded successfully', 201);
  } catch (err) {
    console.error('[QR] createQRCode error:', err);
    return error(res, err.message || 'Failed to upload QR code', 500);
  }
};

/**
 * Get all QR codes (admin)
 */
const getQRCodes = async (req, res) => {
  try {
    const qrCodes = await QRCode.find()
      .populate('uploadedBy', 'name mobile')
      .sort({ createdAt: -1 });
    return success(res, qrCodes);
  } catch (err) {
    return error(res, 'Failed to fetch QR codes', 500);
  }
};

/**
 * Get active QR code (public — for payment page)
 */
const getActiveQRCode = async (req, res) => {
  try {
    const qrCode = await QRCode.findOne({ isActive: true });
    if (!qrCode) {
      return error(res, 'No active QR code found', 404);
    }
    return success(res, qrCode);
  } catch (err) {
    return error(res, 'Failed to fetch active QR code', 500);
  }
};

/**
 * Set a QR code as active (deactivates all others)
 */
const setActiveQRCode = async (req, res) => {
  try {
    const { id } = req.params;
    
    // Deactivate all QR codes first
    await QRCode.updateMany({}, { isActive: false });
    
    // Activate the selected one
    const qrCode = await QRCode.findByIdAndUpdate(id, { isActive: true }, { new: true });
    if (!qrCode) {
      return error(res, 'QR code not found', 404);
    }
    
    return success(res, qrCode, 'QR code activated successfully');
  } catch (err) {
    return error(res, 'Failed to activate QR code', 500);
  }
};

/**
 * Delete a QR code
 */
const deleteQRCode = async (req, res) => {
  try {
    const qrCode = await QRCode.findByIdAndDelete(req.params.id);
    if (!qrCode) {
      return error(res, 'QR code not found', 404);
    }
    return success(res, null, 'QR code deleted successfully');
  } catch (err) {
    return error(res, 'Failed to delete QR code', 500);
  }
};

module.exports = { createQRCode, getQRCodes, getActiveQRCode, setActiveQRCode, deleteQRCode };
