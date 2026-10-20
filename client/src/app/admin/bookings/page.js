'use client';
import { useState, useEffect, useCallback, useRef } from 'react';
import { bookingAPI } from '@/lib/api';
import { buildAssetUrl } from '@/lib/config';
import styles from './bookings.module.css';

const STATUS_BADGES = {
  SEAT_LOCKED: { class: 'badge-warning', label: 'Locked' },
  PENDING_VERIFICATION: { class: 'badge-warning', label: 'Pending' },
  CONFIRMED: { class: 'badge-success', label: 'Confirmed' },
  REJECTED: { class: 'badge-danger', label: 'Rejected' },
  EXPIRED: { class: 'badge-neutral', label: 'Expired' },
  CANCELLED: { class: 'badge-neutral', label: 'Cancelled' },
};

export default function AdminBookings() {
  const [pendingBookings, setPendingBookings] = useState([]);
  const [allBookings, setAllBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(null);
  const [tab, setTab] = useState('pending');
  const [filters, setFilters] = useState({ status: '', date: '', page: 1 });
  const [pagination, setPagination] = useState({ total: 0, pages: 1 });
  const [notification, setNotification] = useState(null);
  const [showScreenshot, setShowScreenshot] = useState(null);
  const [now, setNow] = useState(() => Date.now());
  const prevCountRef = useRef(0);

  // Fetch pending bookings
  const fetchPending = useCallback(async () => {
    try {
      const res = await bookingAPI.getPending();
      const newBookings = res.data || [];
      const newCount = newBookings.length;
      const oldCount = prevCountRef.current;
      prevCountRef.current = newCount;
      
      setPendingBookings(newBookings);
      
      // Show notification if new bookings arrived
      if (newCount > oldCount && oldCount > 0) {
        setNotification(`🔔 ${newCount - oldCount} new booking(s) pending verification!`);
        // Play notification sound
        try {
          const audio = new Audio('data:audio/wav;base64,UklGRl9vT19teleSkBJRkFNVEAAAA==');
          audio.play().catch(() => {});
        } catch {}
        setTimeout(() => setNotification(null), 5000);
      }
    } catch (err) {
      console.error('Failed to fetch pending:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch all bookings with filters
  const fetchAll = useCallback(async () => {
    try {
      const res = await bookingAPI.getAll(filters);
      setAllBookings(res.data?.bookings || []);
      setPagination(res.data?.pagination || { total: 0, pages: 1 });
    } catch (err) {
      console.error('Failed to fetch bookings:', err);
    }
  }, [filters]);

  useEffect(() => {
    fetchPending();
    const interval = setInterval(fetchPending, 30000);
    const clock = setInterval(() => setNow(Date.now()), 60000);
    return () => {
      clearInterval(interval);
      clearInterval(clock);
    };
  }, [fetchPending]);

  useEffect(() => {
    if (tab === 'all') {
      fetchAll();
    }
  }, [tab, fetchAll]);

  const handleConfirm = async (bookingId) => {
    if (!confirm('Confirm this booking? This will generate a ticket and send SMS to passenger.')) return;
    setActionLoading(bookingId);
    try {
      await bookingAPI.confirm(bookingId);
      // Remove from pending list
      setPendingBookings(prev => prev.filter(b => b._id !== bookingId));
      setNotification('✅ Booking confirmed! Ticket generated and SMS sent.');
      setTimeout(() => setNotification(null), 4000);
    } catch (err) {
      alert('Failed to confirm: ' + (err.message || 'Unknown error'));
    } finally {
      setActionLoading(null);
    }
  };

  const handleReject = async (bookingId) => {
    const reason = prompt('Reason for rejection (optional):');
    if (reason === null) return; // User cancelled
    setActionLoading(bookingId);
    try {
      await bookingAPI.reject(bookingId, reason);
      setPendingBookings(prev => prev.filter(b => b._id !== bookingId));
      setNotification('❌ Booking rejected. Seats released and SMS sent.');
      setTimeout(() => setNotification(null), 4000);
    } catch (err) {
      alert('Failed to reject: ' + (err.message || 'Unknown error'));
    } finally {
      setActionLoading(null);
    }
  };

  const handleExportCSV = async () => {
    try {
      const res = await bookingAPI.exportCSV(filters);
      const csvData = res.data || res;
      const blob = new Blob([csvData], { type: 'text/csv;charset=utf-8;' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `bookings-${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert('Export failed: ' + (err.message || 'Unknown error'));
    }
  };

  const getTimeElapsed = (createdAt) => {
    const diff = now - new Date(createdAt).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    return `${Math.floor(hrs / 24)}d ago`;
  };

  return (
    <div>
      {/* Notification banner */}
      {notification && (
        <div className={styles.notification}>
          {notification}
        </div>
      )}

      <div className={styles.header}>
        <h1 className="heading-2">Bookings</h1>
        <div className={styles.tabs}>
          <button
            className={`${styles.tab} ${tab === 'pending' ? styles.tabActive : ''}`}
            onClick={() => setTab('pending')}
          >
            Verification Queue
            {pendingBookings.length > 0 && (
              <span className={styles.tabBadge}>{pendingBookings.length}</span>
            )}
          </button>
          <button
            className={`${styles.tab} ${tab === 'all' ? styles.tabActive : ''}`}
            onClick={() => setTab('all')}
          >
            All Bookings
          </button>
        </div>
      </div>

      {tab === 'pending' ? (
        /* === VERIFICATION QUEUE === */
        pendingBookings.length === 0 ? (
          <div className="empty-state">
            <span className="empty-icon">✅</span>
            <h3 className="heading-3">No pending verifications</h3>
            <p style={{ color: 'var(--text-secondary)' }}>All payments have been verified. New ones will appear here automatically.</p>
          </div>
        ) : (
          <div className={styles.pendingList}>
            {pendingBookings.map((booking) => (
              <div key={booking._id} className={styles.pendingCard}>
                <div className={styles.pendingHeader}>
                  <div>
                    <span className={styles.refCode}>{booking.referenceCode}</span>
                    <span className={styles.pnr}>PNR: {booking.pnr}</span>
                  </div>
                  <span className={styles.timeElapsed}>{getTimeElapsed(booking.createdAt)}</span>
                </div>

                <div className={styles.pendingBody}>
                  <div className={styles.pendingInfo}>
                    <div className={styles.infoRow}>
                      <span>Route</span>
                      <strong>
                        {booking.schedule?.route?.routeNumber} — {booking.schedule?.route?.source} → {booking.schedule?.route?.destination}
                      </strong>
                    </div>
                    <div className={styles.infoRow}>
                      <span>Passenger</span>
                      <strong>{booking.passengerName} ({booking.passengerMobile})</strong>
                    </div>
                    <div className={styles.infoRow}>
                      <span>Seats</span>
                      <strong>{booking.seats?.join(', ')}</strong>
                    </div>
                    <div className={styles.infoRow}>
                      <span>Amount</span>
                      <strong className={styles.amount}>₹{booking.totalFare}</strong>
                    </div>
                    <div className={styles.infoRow}>
                      <span>Bus</span>
                      <strong>{booking.schedule?.bus?.busNumber} ({booking.schedule?.bus?.type})</strong>
                    </div>
                    <div className={styles.infoRow}>
                      <span>Journey Date</span>
                      <strong>{new Date(booking.journeyDate).toLocaleDateString('en-IN')}</strong>
                    </div>
                  </div>

                  {/* Screenshot */}
                  {booking.paymentScreenshot && (
                    <div className={styles.screenshotSection}>
                      <button
                        className="btn btn-outline btn-sm"
                        onClick={() => setShowScreenshot(showScreenshot === booking._id ? null : booking._id)}
                      >
                        📸 {showScreenshot === booking._id ? 'Hide' : 'View'} Screenshot
                      </button>
                      {showScreenshot === booking._id && (
                        <div className={styles.screenshotPreview}>
                          <img
                            src={buildAssetUrl(booking.paymentScreenshot)}
                            alt="Payment screenshot"
                          />
                        </div>
                      )}
                    </div>
                  )}
                </div>

                <div className={styles.pendingActions}>
                  <button
                    className="btn btn-primary"
                    onClick={() => handleConfirm(booking._id)}
                    disabled={actionLoading === booking._id}
                  >
                    {actionLoading === booking._id ? 'Processing...' : '✅ Confirm Payment'}
                  </button>
                  <button
                    className="btn btn-danger"
                    onClick={() => handleReject(booking._id)}
                    disabled={actionLoading === booking._id}
                  >
                    ❌ Reject
                  </button>
                </div>
              </div>
            ))}
          </div>
        )
      ) : (
        /* === ALL BOOKINGS === */
        <div>
          {/* Filters */}
          <div className={styles.filtersRow}>
            <select
              className="form-select"
              value={filters.status}
              onChange={(e) => setFilters({ ...filters, status: e.target.value, page: 1 })}
            >
              <option value="">All Statuses</option>
              <option value="CONFIRMED">Confirmed</option>
              <option value="PENDING_VERIFICATION">Pending</option>
              <option value="REJECTED">Rejected</option>
              <option value="EXPIRED">Expired</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
            <input
              type="date"
              className="form-input"
              value={filters.date}
              onChange={(e) => setFilters({ ...filters, date: e.target.value, page: 1 })}
            />
            <button className="btn btn-outline btn-sm" onClick={handleExportCSV}>
              📥 Export CSV
            </button>
          </div>

          {/* Table */}
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>PNR</th>
                  <th>Ref Code</th>
                  <th>Passenger</th>
                  <th>Route</th>
                  <th>Seats</th>
                  <th>Fare</th>
                  <th>Date</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {allBookings.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-tertiary)' }}>
                      No bookings found
                    </td>
                  </tr>
                ) : (
                  allBookings.map((b) => {
                    const statusInfo = STATUS_BADGES[b.status] || STATUS_BADGES.CANCELLED;
                    return (
                      <tr key={b._id}>
                        <td><strong>{b.pnr}</strong></td>
                        <td style={{ fontFamily: "'Outfit', monospace", fontWeight: 600, color: 'var(--primary-600)' }}>{b.referenceCode}</td>
                        <td>
                          {b.passengerName}<br />
                          <span className="text-xs" style={{ color: 'var(--text-tertiary)' }}>{b.passengerMobile}</span>
                        </td>
                        <td>{b.schedule?.route?.routeNumber} — {b.schedule?.route?.source} → {b.schedule?.route?.destination}</td>
                        <td>{b.seats?.join(', ')}</td>
                        <td><strong>₹{b.totalFare}</strong></td>
                        <td>{new Date(b.journeyDate).toLocaleDateString('en-IN')}</td>
                        <td><span className={`badge ${statusInfo.class}`}>{statusInfo.label}</span></td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {pagination.pages > 1 && (
            <div className={styles.pagination}>
              <button
                className="btn btn-ghost btn-sm"
                disabled={filters.page <= 1}
                onClick={() => setFilters({ ...filters, page: filters.page - 1 })}
              >
                ← Prev
              </button>
              <span className="text-small" style={{ color: 'var(--text-secondary)' }}>
                Page {filters.page} of {pagination.pages} ({pagination.total} total)
              </span>
              <button
                className="btn btn-ghost btn-sm"
                disabled={filters.page >= pagination.pages}
                onClick={() => setFilters({ ...filters, page: filters.page + 1 })}
              >
                Next →
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
