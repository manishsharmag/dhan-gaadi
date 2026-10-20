// ====================================================
// Dhan-Gaadi — Enterprise Intercity Transit & Bus Booking Platform
// ====================================================
const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const helmet = require('helmet');
const path = require('path');
const fs = require('fs');
const http = require('http');
require('dotenv').config();

const app = express();
const server = http.createServer(app);

// ---- Socket.IO Setup ----
const { Server } = require('socket.io');
const jwt = require('jsonwebtoken');
const { getJwtSecret } = require('./helpers/jwtSecret');
const BusTracking = require('./models/BusTracking');
const Schedule = require('./models/Schedule');

// ---- Allowed Origins Resolver ----
const configuredOrigins = (process.env.FRONTEND_URL || '')
  .split(',')
  .map(s => s.trim())
  .filter(Boolean);

const allowedOrigins = [
  'http://localhost:3000',
  'http://localhost:8525',
  'https://haryanabusinfo.in',
  'https://www.haryanabusinfo.in',
  'https://backend.haryanabusinfo.in',
  ...configuredOrigins
];

const corsOriginCheck = (origin, callback) => {
  // Allow requests with no origin (like mobile apps, curl, server-to-server)
  if (!origin) return callback(null, true);
  
  const normalized = origin.replace(/\/+$/, '').toLowerCase();
  const isAllowed = 
    allowedOrigins.some(o => o.toLowerCase() === normalized) ||
    normalized.endsWith('.haryanabusinfo.in') ||
    normalized === 'https://haryanabusinfo.in' ||
    normalized === 'http://haryanabusinfo.in' ||
    normalized.endsWith('.vercel.app') ||
    normalized.endsWith('.hostingersite.com') ||
    normalized.includes('hostingersite.com') ||
    normalized.endsWith('.hostinger.com') ||
    normalized.includes('hostinger.com') ||
    normalized.endsWith('.pages.dev') ||
    normalized.endsWith('.netlify.app') ||
    process.env.CORS_ALLOW_ALL === 'true' ||
    process.env.NODE_ENV !== 'production' ||
    /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(normalized);

  if (isAllowed) {
    return callback(null, true);
  }
  return callback(new Error(`CORS blocked request from origin: ${origin}`));
};

const corsOptions = {
  origin: corsOriginCheck,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Requested-With', 'Accept', 'Origin'],
  credentials: true,
};

const io = new Server(server, {
  cors: {
    origin: corsOriginCheck,
    methods: ['GET', 'POST'],
    credentials: true,
  },
});
app.set('io', io);

// Ensure public directories exist
const publicDir = path.join(__dirname, 'public');
const uploadsDir = path.join(publicDir, 'uploads');
const ticketsDir = path.join(publicDir, 'tickets');

if (!fs.existsSync(publicDir)) fs.mkdirSync(publicDir);
if (!fs.existsSync(uploadsDir)) fs.mkdirSync(uploadsDir);
if (!fs.existsSync(ticketsDir)) fs.mkdirSync(ticketsDir);

// Database connection
const dbConnection = require('./helpers/dbConnection');
dbConnection();

// ---- Security & Parsing Middleware ----
app.use(helmet({
  crossOriginResourcePolicy: { policy: 'cross-origin' }, // Allow serving static files cross-origin
}));
app.use(cors(corsOptions));
app.options('*', cors(corsOptions));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// ---- Logging ----
if (process.env.NODE_ENV !== 'production') {
  app.use(morgan('dev'));
} else {
  app.use(morgan('combined'));
}

// ---- Rate Limiting ----
// Trust the first proxy to ensure express-rate-limit correctly identifies the client IP
app.set('trust proxy', 1);
const { apiLimiter } = require('./middleware/rateLimiter');
app.use('/api', apiLimiter);

// ---- Static Files ----
app.use(express.static(path.join(__dirname, 'public')));
// Serve uploaded images
app.use('/img', express.static(path.join(__dirname, 'public', 'uploads')));
app.use('/uploads', express.static(path.join(__dirname, 'public', 'uploads')));
// Serve generated tickets
app.use('/tickets', express.static(path.join(__dirname, 'public', 'tickets')));
const { downloadTicketPDF } = require('./controllers/bookingController');
app.get('/tickets/:pnr', downloadTicketPDF);
app.get('/ticket/:pnr', downloadTicketPDF);
app.get('/api/ticket/:pnr', downloadTicketPDF);

// ---- Health Check & System Status ----
const sendSystemStatus = (req, res) => {
  res.json({
    success: true,
    message: 'Dhan-Gaadi Fleet OS API',
    version: '2.1.0',
    uptime: process.uptime(),
    timestamp: new Date().toISOString(),
    endpoints: {
      auth: '/api/auth',
      bookings: '/api/bookings',
      buses: '/api/buses',
      routes: '/api/routes',
      schedules: '/api/schedules',
      qr: '/api/qr',
      tracking: '/api/tracking',
      notifications: '/api/notifications',
    }
  });
};

app.get('/', sendSystemStatus);
app.get('/health', sendSystemStatus);
app.get('/status', sendSystemStatus);
app.get('/api', sendSystemStatus);
app.get('/api/status', sendSystemStatus);
app.get('/api/health', sendSystemStatus);

const apiRouter = express.Router();
apiRouter.use('/auth', require('./routes/auth'));
apiRouter.use('/bookings', require('./routes/bookings'));
apiRouter.use('/buses', require('./routes/buses'));
apiRouter.use('/routes', require('./routes/routes'));
apiRouter.use('/schedules', require('./routes/schedules'));
apiRouter.use('/qr', require('./routes/qr'));
apiRouter.use('/tracking', require('./routes/tracking'));
apiRouter.use('/notifications', require('./routes/notifications'));

app.use('/api', apiRouter);
app.use('/', apiRouter);

// ---- Socket.IO Authentication & Events ----
io.use((socket, next) => {
  const token = socket.handshake.auth?.token 
    || socket.handshake.query?.token 
    || socket.handshake.headers?.authorization?.replace('Bearer ', '');
  const secret = getJwtSecret();
  if (token) {
    try {
      const decoded = jwt.verify(token, secret);
      socket.userId = decoded.id;
      socket.userRole = decoded.role;
    } catch (err) {
      // Allow unauthenticated connections for customers viewing tracking
    }
  }
  next();
});

io.on('connection', (socket) => {
  console.log(`[SOCKET] Client connected: ${socket.id}`);

  // Conductor sends location updates
  socket.on('bus:location-update', async (data) => {
    try {
      const staffRoles = ['conductor', 'admin', 'owner', 'superadmin'];
      if (!socket.userRole || !staffRoles.includes(socket.userRole)) {
        return;
      }

      const { scheduleId, lat, lng, speed, heading, accuracy } = data || {};
      if (!scheduleId || lat === undefined || lng === undefined) return;

      const updateFields = {
        location: { lat: Number(lat), lng: Number(lng) },
        speed: Number(speed) || 0,
        heading: Number(heading) || 0,
        accuracy: Number(accuracy) || 0,
        isActive: true,
        lastUpdated: new Date(),
      };
      if (socket.userId) {
        updateFields.conductor = socket.userId;
      }

      await BusTracking.findOneAndUpdate(
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

      // Broadcast to all clients watching this schedule
      io.to(`track:${scheduleId}`).emit('bus:location', {
        scheduleId,
        location: { lat: Number(lat), lng: Number(lng) },
        speed: Number(speed) || 0,
        heading: Number(heading) || 0,
        lastUpdated: new Date(),
      });
    } catch (err) {
      console.error('[SOCKET] location-update error:', err.message);
    }
  });

  // Conductor stops tracking
  socket.on('bus:tracking-stop', async (data) => {
    try {
      const staffRoles = ['conductor', 'admin', 'owner', 'superadmin'];
      if (!socket.userRole || !staffRoles.includes(socket.userRole)) {
        return;
      }

      const { scheduleId } = data || {};
      if (!scheduleId) return;

      await BusTracking.findOneAndUpdate(
        { schedule: scheduleId, isActive: true },
        { $set: { isActive: false } }
      );
      io.to(`track:${scheduleId}`).emit('bus:tracking-ended', { scheduleId });
      console.log(`[SOCKET] Tracking stopped for schedule ${scheduleId}`);
    } catch (err) {
      console.error('[SOCKET] tracking-stop error:', err.message);
    }
  });

  // Customer/owner subscribes to a bus
  socket.on('bus:subscribe', (data) => {
    const { scheduleId } = data;
    socket.join(`track:${scheduleId}`);
    console.log(`[SOCKET] ${socket.id} subscribed to track:${scheduleId}`);
  });

  // Customer/owner unsubscribes
  socket.on('bus:unsubscribe', (data) => {
    const { scheduleId } = data;
    socket.leave(`track:${scheduleId}`);
  });

  socket.on('disconnect', () => {
    console.log(`[SOCKET] Client disconnected: ${socket.id}`);
  });
});

// ---- 404 Handler ----
app.use((req, res) => {
  res.status(404).json({
    success: false,
    data: null,
    message: `Route ${req.originalUrl} not found`,
  });
});

// ---- Global Error Handler ----
app.use((err, req, res, next) => {
  console.error('[ERROR]', err.stack || err.message);
  
  // Multer errors
  if (err.code === 'LIMIT_FILE_SIZE') {
    return res.status(400).json({
      success: false,
      data: null,
      message: 'File too large. Maximum size is 10MB.',
    });
  }

  if (err.message && err.message.toLowerCase().includes('image')) {
    return res.status(400).json({
      success: false,
      data: null,
      message: err.message,
    });
  }
  
  res.status(err.status || 500).json({
    success: false,
    data: null,
    message: err.message || 'Something went wrong',
  });
});

// ---- Start Server ----
const PORT = process.env.PORT || 8525;

server.listen(PORT, () => {
  console.log(`
  ╔═══════════════════════════════════════════════╗
  ║   Dhan-Gaadi Fleet OS Server v2.1             ║
  ║   Server running on port ${PORT}               ║
  ║   Socket.IO: enabled                          ║
  ║   Environment: ${process.env.NODE_ENV || 'development'}             ║
  ╚═══════════════════════════════════════════════╝
  `);
});

module.exports = { app, server, io };

