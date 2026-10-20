import axios from 'axios';
import { API_BASE_URL } from './config';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor — attach JWT token
api.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('hr_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

// Response interceptor — return full response and handle errors centrally
api.interceptors.response.use(
  (response) => {
    // Backend wraps response in { success: true, data: ... }
    if (response.data && response.data.success === true && response.data.data !== undefined) {
      response.data = response.data.data;
    }
    return response;
  },
  (error) => {
    const message = error.response?.data?.message || error.message || 'Something went wrong';
    // Auto logout on 401
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      localStorage.removeItem('hr_token');
      localStorage.removeItem('hr_user');
      if (!window.location.pathname.includes('/login')) {
        const next = `${window.location.pathname}${window.location.search}`;
        window.location.href = `/login?next=${encodeURIComponent(next)}`;
      }
    }
    return Promise.reject({ message, status: error.response?.status, raw: error });
  }
);

// --- Auth API ---
export const authAPI = {
  login: (mobile, password) => api.post('/auth/login', { mobile, password }),
  register: (mobile, name, password) => api.post('/auth/register', { mobile, name, password }),
  resetPassword: (mobile, newPassword) => api.post('/auth/reset-password', { mobile, newPassword }),
  getProfile: () => api.get('/auth/profile'),
  updateProfile: (data) => api.put('/auth/profile', data),
  // Conductor Management (Admin)
  createConductor: (data) => api.post('/auth/admin/conductors', data),
  getConductors: () => api.get('/auth/admin/conductors'),
  deleteConductor: (id) => api.delete(`/auth/admin/conductors/${id}`),
};

// --- Schedule/Search API ---
export const scheduleAPI = {
  search: (params) => api.get('/schedules/search', { params }),
  getAll: (params) => api.get('/schedules', { params }),
  getById: (id) => api.get(`/schedules/${id}`),
  create: (data) => api.post('/schedules', data),
  update: (id, data) => api.put(`/schedules/${id}`, data),
  delete: (id) => api.delete(`/schedules/${id}`),
};

// --- Booking API ---
export const bookingAPI = {
  getSeatAvailability: (scheduleId, date) => api.get('/bookings/seats', { params: { scheduleId, date } }),
  create: (data) => api.post('/bookings', data),
  submitPayment: (bookingId, formData) => api.post(`/bookings/${bookingId}/payment`, formData),
  instantDemoPay: (bookingId) => api.post(`/bookings/${bookingId}/instant-demo-pay`),
  cancel: (bookingId) => api.put(`/bookings/${bookingId}/cancel`),
  getMyBookings: () => api.get('/bookings/my-bookings'),
  getById: (id) => api.get(`/bookings/${id}`),
  getByPNR: (pnr) => api.get(`/bookings/pnr/${encodeURIComponent(String(pnr || '').trim())}`),
  // Admin
  getPending: () => api.get('/bookings/admin/pending'),
  confirm: (bookingId) => api.put(`/bookings/admin/${bookingId}/confirm`),
  reject: (bookingId, reason) => api.put(`/bookings/admin/${bookingId}/reject`, { reason }),
  getAll: (params) => api.get('/bookings/admin/all', { params }),
  getStats: () => api.get('/bookings/admin/dashboard'),
  exportCSV: (params) => api.get('/bookings/admin/export', { params, responseType: 'blob' }),
};

// --- Route API ---
export const routeAPI = {
  getCities: () => api.get('/routes/cities'),
  search: (params) => api.get('/routes/search', { params }),
  getAll: (params) => api.get('/routes', { params }),
  getById: (id) => api.get(`/routes/${id}`),
  create: (data) => api.post('/routes', data),
  update: (id, data) => api.put(`/routes/${id}`, data),
  delete: (id) => api.delete(`/routes/${id}`),
};

// --- Bus API ---
export const busAPI = {
  getAll: (params) => api.get('/buses', { params }),
  getById: (id) => api.get(`/buses/${id}`),
  create: (data) => api.post('/buses', data),
  update: (id, data) => api.put(`/buses/${id}`, data),
  delete: (id) => api.delete(`/buses/${id}`),
};

// --- QR Code API ---
export const qrAPI = {
  getActive: () => api.get('/qr/active'),
  getAll: () => api.get('/qr'),
  upload: (formData) => api.post('/qr', formData),
  activate: (id) => api.put(`/qr/${id}/activate`),
  delete: (id) => api.delete(`/qr/${id}`),
};

// --- Tracking API ---
export const trackingAPI = {
  getLiveBus: (scheduleId) => api.get(`/tracking/${scheduleId}`),
  getActiveBuses: () => api.get('/tracking/active/all'),
  getSchedulePolyline: (scheduleId) => api.get(`/tracking/${scheduleId}/polyline`),
};

// --- Ticket API ---
export const ticketAPI = {
  getByPNR: (pnr) => api.get(`/ticket/${encodeURIComponent(String(pnr || '').trim())}`, { responseType: 'blob' }),
};

export default api;
