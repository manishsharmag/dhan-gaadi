const Route = require('../models/Route');
const { success, error } = require('../utils/response');

const addRoute = async (req, res) => {
  try {
    const route = await Route.create(req.body);
    return success(res, route, 'Route added successfully', 201);
  } catch (err) {
    if (err.code === 11000) {
      return error(res, 'Route with this number already exists', 409);
    }
    return error(res, 'Failed to add route', 500);
  }
};

const escapeRegex = (str) => String(str).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

const getRoutes = async (req, res) => {
  try {
    const { status, source, destination } = req.query;
    const filter = {};
    if (status) filter.status = status;
    if (source) filter.source = new RegExp(escapeRegex(source.trim()), 'i');
    if (destination) filter.destination = new RegExp(escapeRegex(destination.trim()), 'i');
    
    const routes = await Route.find(filter).sort({ routeNumber: 1 });
    return success(res, routes);
  } catch (err) {
    return error(res, 'Failed to fetch routes', 500);
  }
};

const getRoute = async (req, res) => {
  try {
    const route = await Route.findById(req.params.id);
    if (!route) return error(res, 'Route not found', 404);
    return success(res, route);
  } catch (err) {
    return error(res, 'Failed to fetch route', 500);
  }
};

const updateRoute = async (req, res) => {
  try {
    const route = await Route.findByIdAndUpdate(req.params.id, req.body, { new: true, runValidators: true });
    if (!route) return error(res, 'Route not found', 404);
    return success(res, route, 'Route updated successfully');
  } catch (err) {
    return error(res, 'Failed to update route', 500);
  }
};

const deleteRoute = async (req, res) => {
  try {
    const route = await Route.findByIdAndDelete(req.params.id);
    if (!route) return error(res, 'Route not found', 404);
    return success(res, null, 'Route deleted successfully');
  } catch (err) {
    return error(res, 'Failed to delete route', 500);
  }
};

/**
 * Search routes — public endpoint for users searching source→destination
 */
const searchRoutes = async (req, res) => {
  try {
    const { source, destination } = req.query;
    
    if (!source || !destination) {
      return error(res, 'Source and destination are required', 400);
    }
    
    const routes = await Route.find({
      source: new RegExp(escapeRegex(source.trim()), 'i'),
      destination: new RegExp(escapeRegex(destination.trim()), 'i'),
      status: 'active',
    });
    
    return success(res, routes);
  } catch (err) {
    return error(res, 'Failed to search routes', 500);
  }
};

/**
 * Get distinct active cities (sources, destinations, stops)
 */
const getCities = async (req, res) => {
  const fallbackCities = [
    'Hisar', 'Chandigarh', 'Delhi', 'Rohtak', 'Ambala',
    'Karnal', 'Panipat', 'Jind', 'Kurukshetra', 'Sonipat',
    'Bahadurgarh', 'Hansi', 'Gurugram', 'Faridabad', 'Rewari'
  ];
  try {
    const routes = await Route.find({ status: 'active' }).select('source destination stops').lean();
    const citySet = new Set(fallbackCities);
    if (Array.isArray(routes)) {
      routes.forEach((r) => {
        if (r.source) citySet.add(r.source.trim());
        if (r.destination) citySet.add(r.destination.trim());
        if (Array.isArray(r.stops)) {
          r.stops.forEach((s) => {
            if (s && s.name) citySet.add(s.name.trim());
          });
        }
      });
    }
    const sortedCities = Array.from(citySet).filter(Boolean).sort();
    return success(res, sortedCities);
  } catch (err) {
    console.warn('[ROUTE] getCities DB query failed, returning fallback cities:', err.message);
    return success(res, fallbackCities.sort());
  }
};

module.exports = { addRoute, getRoutes, getRoute, updateRoute, deleteRoute, searchRoutes, getCities };
