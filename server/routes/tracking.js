const express = require('express');
const router = express.Router();
const { protect, staffOnly } = require('../middleware/auth');
const BusTracking = require('../models/BusTracking');
const Schedule = require('../models/Schedule');
const { success, error } = require('../utils/response');

/**
 * GET /tracking/active/all — Get all actively tracked buses (for public fleet view & live radar)
 * Note: Placed above /:scheduleId to avoid route parameter collision.
 */
router.get('/active/all', async (req, res) => {
  try {
    const trackings = await BusTracking.find({ isActive: true })
      .populate({
        path: 'schedule',
        populate: [
          { path: 'route', select: 'routeNumber name source destination' },
          { path: 'bus', select: 'busNumber type' },
          { path: 'conductor', select: 'name mobile photo role' },
        ],
      })
      .sort({ lastUpdated: -1 });

    return success(res, trackings);
  } catch (err) {
    console.error('[TRACKING] getActiveAll error:', err);
    return error(res, 'Failed to fetch active trackings', 500);
  }
});

/**
 * GET /tracking/:scheduleId — Get latest live location for a schedule
 * Public endpoint so customers can check bus position
 */
router.get('/:scheduleId', async (req, res) => {
  try {
    const tracking = await BusTracking.findOne({
      schedule: req.params.scheduleId,
      isActive: true,
    }).populate({
      path: 'schedule',
      populate: [
        { path: 'route', select: 'routeNumber name source destination stops totalDistance' },
        { path: 'bus', select: 'busNumber type' },
        { path: 'conductor', select: 'name mobile photo role' },
      ],
    });

    if (!tracking) {
      return error(res, 'No active tracking for this bus', 404);
    }

    return success(res, {
      location: tracking.location,
      speed: tracking.speed,
      heading: tracking.heading,
      currentStop: tracking.currentStop,
      nextStop: tracking.nextStop,
      stopsReached: tracking.stopsReached,
      lastUpdated: tracking.lastUpdated,
      schedule: tracking.schedule,
      isActive: tracking.isActive,
    });
  } catch (err) {
    console.error('[TRACKING] getLocation error:', err);
    return error(res, 'Failed to fetch bus location', 500);
  }
});

/**
 * GET /tracking/:scheduleId/eta — Get ETA to each remaining stop
 */
router.get('/:scheduleId/eta', async (req, res) => {
  try {
    const tracking = await BusTracking.findOne({
      schedule: req.params.scheduleId,
      isActive: true,
    });

    if (!tracking) {
      return error(res, 'No active tracking for this bus', 404);
    }

    const schedule = await Schedule.findById(req.params.scheduleId)
      .populate('route');

    if (!schedule || !schedule.route) {
      return error(res, 'Schedule not found', 404);
    }

    const stops = schedule.route.stops || [];
    const avgSpeed = tracking.speed > 0 ? tracking.speed : 40; // default 40 km/h

    // Calculate distance from bus to each stop using distanceFromSource
    const reachedNames = (tracking.stopsReached || []).map(s => s.name);
    const etas = stops
      .sort((a, b) => a.order - b.order)
      .map(stop => {
        const reached = reachedNames.includes(stop.name);
        // Simple ETA based on distance and current speed
        let etaMinutes = null;
        if (!reached && stop.distanceFromSource > 0) {
          // Rough estimate: how far bus has traveled vs stop distance
          const busDistanceTraveled = getBusDistanceTraveled(tracking, stops);
          const remainingKm = Math.max(0, stop.distanceFromSource - busDistanceTraveled);
          etaMinutes = Math.round((remainingKm / avgSpeed) * 60);
        }
        return {
          name: stop.name,
          order: stop.order,
          distanceFromSource: stop.distanceFromSource,
          reached,
          etaMinutes,
        };
      });

    return success(res, {
      etas,
      currentSpeed: tracking.speed,
      lastUpdated: tracking.lastUpdated,
    });
  } catch (err) {
    console.error('[TRACKING] getETA error:', err);
    return error(res, 'Failed to calculate ETA', 500);
  }
});

// Haversine formula to calculate distance in km between two GPS coordinates
function haversineDistance(lat1, lon1, lat2, lon2) {
  const toRad = (x) => (x * Math.PI) / 180;
  const R = 6371; // Earth radius in km
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

// Helper: estimate how far bus has traveled based on reached stops or closest GPS stop
function getBusDistanceTraveled(tracking, stops) {
  const reachedNames = (tracking.stopsReached || []).map(s => s.name);
  const sortedStops = [...stops].sort((a, b) => a.order - b.order);

  let lastReachedDistance = 0;
  for (const stop of sortedStops) {
    if (reachedNames.includes(stop.name)) {
      lastReachedDistance = Math.max(lastReachedDistance, stop.distanceFromSource || 0);
    }
  }

  if (lastReachedDistance > 0) return lastReachedDistance;

  // If live location exists, correlate with nearest stop coordinates
  if (tracking.location && tracking.location.lat && tracking.location.lng) {
    let minDistance = Infinity;
    let estimatedTraveled = 0;

    for (const stop of sortedStops) {
      if (stop.lat && stop.lng) {
        const d = haversineDistance(tracking.location.lat, tracking.location.lng, stop.lat, stop.lng);
        if (d < minDistance) {
          minDistance = d;
          estimatedTraveled = stop.distanceFromSource || 0;
        }
      }
    }

    if (minDistance < 30) {
      return estimatedTraveled;
    }
  }

  return 0;
}

/**
 * POST /tracking/update — HTTP fallback for conductor location broadcasting
 */
router.post('/update', protect, staffOnly, async (req, res) => {
  try {
    const { scheduleId, lat, lng, speed, heading, accuracy } = req.body || {};
    if (!scheduleId || lat === undefined || lng === undefined) {
      return error(res, 'scheduleId, lat, and lng are required', 400);
    }

    const updateFields = {
      location: { lat: Number(lat), lng: Number(lng) },
      speed: Number(speed) || 0,
      heading: Number(heading) || 0,
      accuracy: Number(accuracy) || 0,
      isActive: true,
      lastUpdated: new Date(),
    };
    if (req.user && req.user._id) {
      updateFields.conductor = req.user._id;
    }

    const tracking = await BusTracking.findOneAndUpdate(
      { schedule: scheduleId, isActive: true },
      {
        $set: updateFields,
        $setOnInsert: {
          schedule: scheduleId,
          startedAt: new Date(),
        },
      },
      { upsert: true, new: true }
    );

    // Broadcast through socket if available
    const io = req.app.get('io');
    if (io) {
      io.to(`track:${scheduleId}`).emit('bus:location', {
        scheduleId,
        location: { lat: Number(lat), lng: Number(lng) },
        speed: Number(speed) || 0,
        heading: Number(heading) || 0,
        lastUpdated: new Date(),
      });
    }

    return success(res, tracking, 'Location updated successfully');
  } catch (err) {
    console.error('[TRACKING] POST /update error:', err);
    return error(res, 'Failed to update location', 500);
  }
});

/**
 * POST /tracking/stop — Stop tracking broadcast for a schedule
 */
router.post('/stop', protect, staffOnly, async (req, res) => {
  try {
    const { scheduleId } = req.body || {};
    if (!scheduleId) return error(res, 'scheduleId is required', 400);

    await BusTracking.findOneAndUpdate(
      { schedule: scheduleId, isActive: true },
      { $set: { isActive: false, lastUpdated: new Date() } }
    );

    const io = req.app.get('io');
    if (io) {
      io.to(`track:${scheduleId}`).emit('bus:tracking-ended', { scheduleId });
    }

    return success(res, null, 'Tracking ended');
  } catch (err) {
    console.error('[TRACKING] POST /stop error:', err);
    return error(res, 'Failed to stop tracking', 500);
  }
});

module.exports = router;
