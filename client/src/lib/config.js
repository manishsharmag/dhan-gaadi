const DEFAULT_API_BASE_URL = 'http://localhost:8525/api';

const stripTrailingSlash = (value) => (value || '').replace(/\/+$/, '');
let rawBackend = stripTrailingSlash(process.env.NEXT_PUBLIC_API_URL || 'https://backend.haryanabusinfo.in');
if (rawBackend.endsWith('/api')) {
  rawBackend = rawBackend.slice(0, -4);
}

export const BACKEND_ORIGIN = rawBackend;
// In browser, use same-origin '/api' which is proxied server-side by Next.js, eliminating all CORS issues across any domain
export const API_BASE_URL = typeof window !== 'undefined' ? '/api' : `${BACKEND_ORIGIN}/api`;

export const ADMIN_ROLES = ['owner', 'admin', 'superadmin'];
export const STAFF_ROLES = [...ADMIN_ROLES, 'conductor'];

export const isAdminRole = (role) => ADMIN_ROLES.includes(role);
export const isStaffRole = (role) => STAFF_ROLES.includes(role);

export const buildBackendUrl = (path) => {
  if (!path) return '';
  if (/^https?:\/\//i.test(path)) return path;
  return `${BACKEND_ORIGIN}/${String(path).replace(/^\/+/, '')}`;
};

export const buildAssetUrl = buildBackendUrl;

export const buildTicketPdfUrl = (booking) => {
  if (!booking) return '';
  if (booking.ticketUrl) return buildBackendUrl(booking.ticketUrl);
  if (booking.pnr) return buildBackendUrl(`/tickets/${booking.pnr}.pdf`);
  if (booking.referenceCode) return buildBackendUrl(`/tickets/${booking.referenceCode}.pdf`);
  if (booking._id) return buildBackendUrl(`/tickets/${booking._id}.pdf`);
  return '';
};
