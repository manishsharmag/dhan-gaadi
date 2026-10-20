'use client';
import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import Navbar from '@/components/Navbar';
import Footer from '@/components/Footer';
import { useAuth } from '@/lib/auth';
import { bookingAPI } from '@/lib/api';
import { buildTicketPdfUrl } from '@/lib/config';
import styles from './profile.module.css';

const STATUS_BADGES = {
  SEAT_LOCKED: { class: 'badge-warning', label: 'Seats Locked' },
  PENDING_VERIFICATION: { class: 'badge-warning', label: 'Pending Verification' },
  CONFIRMED: { class: 'badge-success', label: 'Confirmed' },
  REJECTED: { class: 'badge-danger', label: 'Rejected' },
  EXPIRED: { class: 'badge-neutral', label: 'Expired' },
  CANCELLED: { class: 'badge-neutral', label: 'Cancelled' },
};

export default function ProfilePage() {
  const router = useRouter();
  const { user, isAuthenticated, loading: authLoading } = useAuth();
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchBookings = useCallback(async () => {
    try {
      const res = await bookingAPI.getMyBookings();
      setBookings(res.data || []);
    } catch (err) { console.error(err); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => {
    if (authLoading) return;
    if (!isAuthenticated) {
      if (typeof window !== 'undefined') {
        const next = encodeURIComponent(window.location.pathname + window.location.search);
        router.push(`/login?next=${next}`);
      } else {
        router.push('/login');
      }
      return;
    }
    fetchBookings();
  }, [isAuthenticated, authLoading, fetchBookings, router]);

  const [cancellingId, setCancellingId] = useState(null);

  const handleCancelBooking = async (bookingId) => {
    if (!window.confirm('Are you sure you want to cancel this booking? The locked/reserved seats will be released immediately.')) {
      return;
    }
    try {
      setCancellingId(bookingId);
      await bookingAPI.cancel(bookingId);
      await fetchBookings();
    } catch (err) {
      alert('Failed to cancel booking: ' + (err.message || 'Unknown error'));
    } finally {
      setCancellingId(null);
    }
  };

  return (
    <>
      <Navbar />
      <main className={styles.page}>
        <div className="container">
          <div className={styles.profileHeader}>
            <div className={styles.avatar}>{user?.name?.[0]?.toUpperCase() || '👤'}</div>
            <div>
              <h1 className="heading-2">{user?.name || 'Passenger'}</h1>
              <p style={{ color: 'var(--text-secondary)' }}>+91 {user?.mobile}</p>
              {['admin', 'owner', 'superadmin'].includes(user?.role?.toLowerCase()) && (
                <div style={{ marginTop: '0.5rem', display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                  <span className="badge badge-warning" style={{ fontWeight: 'bold' }}>
                    {user.role.toUpperCase()}
                  </span>
                  <Link href="/admin" className="btn btn-sm btn-primary" style={{ padding: '0.25rem 0.75rem', fontSize: '0.8rem' }}>
                    🛡️ Open Admin Dashboard
                  </Link>
                </div>
              )}
              {user?.role?.toLowerCase() === 'conductor' && (
                <div style={{ marginTop: '0.5rem', display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                  <span className="badge badge-info" style={{ fontWeight: 'bold' }}>
                    👮 CONDUCTOR ON DUTY
                  </span>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                    Please use your Dhan-Gaadi Conductor Mobile App to broadcast live GPS and issue POS tickets.
                  </span>
                </div>
              )}
            </div>
          </div>
          <h2 className="heading-3" style={{ marginBottom: '1.5rem' }}>My Bookings</h2>
          {loading ? (
            <div className="loading-overlay"><div className="spinner" /></div>
          ) : bookings.length === 0 ? (
            <div className="empty-state">
              <span className="empty-icon">🎫</span>
              <h3 className="heading-3">No bookings yet</h3>
              <p style={{ color: 'var(--text-secondary)' }}>Your bookings will appear here</p>
              <button onClick={() => router.push('/')} className="btn btn-primary">Book a Bus</button>
            </div>
          ) : (
            <div className={styles.bookingList}>
              {bookings.map((b) => {
                const statusInfo = STATUS_BADGES[b.status] || STATUS_BADGES.CANCELLED;
                const canCancel = ['CONFIRMED', 'PENDING_VERIFICATION', 'SEAT_LOCKED'].includes(b.status);
                return (
                  <div key={b._id} className={styles.bookingCard}>
                    <div className={styles.bookingTop}>
                      <div>
                        <span className={styles.pnr}>PNR: {b.pnr}</span>
                        <span className={styles.refCode}>{b.referenceCode}</span>
                      </div>
                      <span className={`badge ${statusInfo.class}`}>{statusInfo.label}</span>
                    </div>
                    <div className={styles.bookingBody}>
                      <div className={styles.bookingRoute}>
                        <strong>{b.schedule?.route?.source}</strong>
                        <span>→</span>
                        <strong>{b.schedule?.route?.destination}</strong>
                      </div>
                      <div className={styles.bookingMeta}>
                        <span>Seats: {b.seats?.join(', ')}</span>
                        <span>Date: {new Date(b.journeyDate).toLocaleDateString('en-IN')}</span>
                        <span>Fare: ₹{b.totalFare}</span>
                      </div>

                      {/* Assigned Conductor Card */}
                      {b.schedule?.conductor && (
                        <div style={{
                          marginTop: '0.75rem',
                          padding: '0.6rem 0.85rem',
                          background: '#f0fdfa',
                          border: '1px solid #ccfbf1',
                          borderRadius: '8px',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          gap: '0.75rem',
                          flexWrap: 'wrap'
                        }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                            <div style={{
                              width: '32px', height: '32px', borderRadius: '50%',
                              background: 'var(--primary-100, #ccfbf1)', display: 'flex',
                              alignItems: 'center', justifyContent: 'center', fontSize: '1rem',
                              overflow: 'hidden'
                            }}>
                              {b.schedule.conductor.photo ? (
                                <img src={b.schedule.conductor.photo} alt={b.schedule.conductor.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                              ) : '👮'}
                            </div>
                            <div>
                              <div style={{ fontWeight: 600, fontSize: '0.85rem', color: 'var(--primary-900, #134e4a)' }}>
                                {b.schedule.conductor.name}
                              </div>
                              <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                                Conductor • +91 {b.schedule.conductor.mobile}
                              </div>
                            </div>
                          </div>
                          <div style={{ display: 'flex', gap: '0.4rem' }}>
                            <a href={`tel:${b.schedule.conductor.mobile}`} className="btn btn-outline btn-sm" style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem' }}>
                              📞 Call
                            </a>
                            <a
                              href={`https://api.whatsapp.com/send?phone=91${b.schedule.conductor.mobile.replace(/\D/g, '').slice(-10)}&text=${encodeURIComponent(`Hello Conductor ${b.schedule.conductor.name}, I am a passenger with PNR ${b.pnr}.`)}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="btn btn-sm"
                              style={{ background: '#25D366', color: '#fff', padding: '0.2rem 0.5rem', fontSize: '0.75rem' }}
                            >
                              WhatsApp
                            </a>
                          </div>
                        </div>
                      )}

                      {/* Rejection Alert */}
                      {b.status === 'REJECTED' && (
                        <div style={{
                          marginTop: '0.75rem',
                          padding: '0.75rem 1rem',
                          background: '#fef2f2',
                          border: '1px solid #fecaca',
                          borderRadius: '8px',
                          color: '#991b1b',
                          fontSize: '0.875rem'
                        }}>
                          <strong>✕ Booking Rejected</strong>
                          <p style={{ margin: '0.25rem 0 0', color: '#b91c1c' }}>
                            Reason: {b.rejectionReason || 'Payment or booking details could not be verified.'}
                          </p>
                          <small style={{ color: '#6b7280', display: 'block', marginTop: '4px' }}>
                            If payment was deducted from your account, it will be reconciled. Please contact Dhan-Gaadi helpline.
                          </small>
                        </div>
                      )}

                      {/* Pending Verification Notice */}
                      {b.status === 'PENDING_VERIFICATION' && (
                        <div style={{
                          marginTop: '0.75rem',
                          padding: '0.75rem 1rem',
                          background: '#fffbeb',
                          border: '1px solid #fde68a',
                          borderRadius: '8px',
                          color: '#92400e',
                          fontSize: '0.875rem'
                        }}>
                          <strong>⏳ Payment Verification In Progress</strong>
                          <p style={{ margin: '0.25rem 0 0', color: '#b45309' }}>
                            Your seats are locked. Dhan-Gaadi Fleet staff is verifying your payment UTR. Click &quot;View Provisional Pass&quot; to inspect details.
                          </p>
                        </div>
                      )}
                    </div>
                    
                    <div className={styles.bookingActions}>
                      {b.status === 'CONFIRMED' && (
                        <>
                          <Link href={`/ticket/${b.pnr}`} className="btn btn-primary btn-sm">
                            🎫 Boarding Pass
                          </Link>
                          {b.ticketUrl && (
                            <a href={buildTicketPdfUrl(b)} className="btn btn-secondary btn-sm" target="_blank" rel="noopener noreferrer">
                              📥 PDF
                            </a>
                          )}
                        </>
                      )}
                      {b.status === 'PENDING_VERIFICATION' && (
                        <Link href={`/ticket/${b.pnr}`} className="btn btn-primary btn-sm" style={{ background: '#d97706' }}>
                          ⏳ View Provisional Pass
                        </Link>
                      )}
                      {b.status === 'REJECTED' && (
                        <Link href={`/ticket/${b.pnr}`} className="btn btn-outline btn-sm" style={{ borderColor: '#dc2626', color: '#dc2626' }}>
                          ✕ View Rejection Details
                        </Link>
                      )}
                      {b.status === 'SEAT_LOCKED' && (
                        <button onClick={() => router.push(`/payment?bookingId=${b._id}`)} className="btn btn-accent btn-sm">
                          💳 Pay Now
                        </button>
                      )}
                      {b.schedule?._id && b.status !== 'CANCELLED' && b.status !== 'REJECTED' && (
                        <button onClick={() => router.push(`/track/${b.schedule._id}`)} className="btn btn-outline btn-sm">
                          📍 Track Bus
                        </button>
                      )}
                      {canCancel && (
                        <button 
                          onClick={() => handleCancelBooking(b._id)} 
                          className="btn btn-ghost btn-sm"
                          disabled={cancellingId === b._id}
                          style={{ color: 'var(--danger)', marginLeft: 'auto' }}
                        >
                          {cancellingId === b._id ? 'Cancelling...' : 'Cancel Booking'}
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </main>
      <Footer />
    </>
  );
}
