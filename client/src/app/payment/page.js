'use client';
import { useState, useEffect, useCallback, useRef, Suspense, use } from 'react';
import { useRouter } from 'next/navigation';
import Navbar from '@/components/Navbar';
import { useAuth } from '@/lib/auth';
import { bookingAPI, qrAPI } from '@/lib/api';
import { buildAssetUrl } from '@/lib/config';
import styles from './payment.module.css';
import { useSearchParams } from 'next/navigation';

function PaymentContent({ explicitBookingId }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const bookingId = explicitBookingId || searchParams.get('bookingId');
  const { isAuthenticated, loading: authLoading } = useAuth();
  const fileInputRef = useRef(null);
  
  const [booking, setBooking] = useState(null);
  const [qrCode, setQrCode] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [instantDemoLoading, setInstantDemoLoading] = useState(false);
  const [instantDemoStep, setInstantDemoStep] = useState('');
  const [error, setError] = useState('');
  const [timeLeft, setTimeLeft] = useState(0);
  const [screenshot, setScreenshot] = useState(null);
  const [submitted, setSubmitted] = useState(false);
  const [copiedUpi, setCopiedUpi] = useState(false);
  const upiTimerRef = useRef(null);

  useEffect(() => {
    return () => {
      if (upiTimerRef.current) clearTimeout(upiTimerRef.current);
    };
  }, []);

  const fetchData = useCallback(async () => {
    if (!bookingId) return;
    try {
      const [bookingRes, qrRes] = await Promise.all([
        bookingAPI.getById(bookingId),
        qrAPI.getActive(),
      ]);
      setBooking(bookingRes.data);
      setQrCode(qrRes.data);
      if (bookingRes.data?.status === 'CONFIRMED') {
        router.push(`/ticket/${bookingRes.data.pnr}`);
        return;
      }
      if (bookingRes.data?.status === 'PENDING_VERIFICATION') setSubmitted(true);
    } catch (err) {
      setError(err.message || 'Failed to load payment page');
    } finally {
      setLoading(false);
    }
  }, [bookingId, router]);

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
    if (bookingId) {
      fetchData();
    }
  }, [bookingId, isAuthenticated, authLoading, fetchData, router]);

  useEffect(() => {
    if (!booking?.seatLockExpiry) return;
    const interval = setInterval(() => {
      const remaining = Math.max(0, new Date(booking.seatLockExpiry) - new Date());
      setTimeLeft(remaining);
      if (remaining <= 0) {
        clearInterval(interval);
        setError('Seat lock has expired! Your seats have been released.');
      }
    }, 1000);
    return () => clearInterval(interval);
  }, [booking?.seatLockExpiry]);

  const handleInstantDemoPayment = async () => {
    setInstantDemoLoading(true);
    setError('');
    setInstantDemoStep('Connecting to Dhan-Gaadi Instant Pay switch...');
    try {
      await new Promise((r) => setTimeout(r, 400));
      setInstantDemoStep('Payment authorized. Generating official QR Boarding Pass...');
      const res = await bookingAPI.instantDemoPay(bookingId);
      setInstantDemoStep('Booking Confirmed! Loading your e-Ticket...');
      await new Promise((r) => setTimeout(r, 400));
      const pnr = res.data?.pnr || booking?.pnr;
      router.push(`/ticket/${pnr}`);
    } catch (err) {
      setError(err.message || 'Failed to complete demo payment');
      setInstantDemoLoading(false);
      setInstantDemoStep('');
    }
  };

  const handleSubmitPayment = async () => {
    setSubmitting(true);
    setError('');
    try {
      const formData = new FormData();
      if (screenshot) formData.append('screenshot', screenshot);
      await bookingAPI.submitPayment(bookingId, formData);
      setSubmitted(true);
    } catch (err) {
      setError(err.message || 'Failed to submit payment');
    } finally {
      setSubmitting(false);
    }
  };

  const formatTime = (ms) => {
    const mins = Math.floor(ms / 60000);
    const secs = Math.floor((ms % 60000) / 1000);
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const isUrgent = timeLeft < 120000;

  if (loading) return <><Navbar /><div className="loading-overlay"><div className="spinner" /></div></>;

  if (submitted) {
    return (
      <>
        <Navbar />
        <main className={styles.page}>
          <div className={styles.confirmCard}>
            <div className={styles.confirmIcon}>⏳</div>
            <h1 className="heading-2">Payment Submitted</h1>
            <p style={{ color: 'var(--text-secondary)', maxWidth: 400, textAlign: 'center', lineHeight: '1.6' }}>
              Your ticket will be confirmed soon once our staff verifies receipt of payment. You can track the status in <strong>My Bookings</strong>.
            </p>
            <div className={styles.confirmDetails}>
              <div className={styles.confirmRow}>
                <span>Reference Code</span>
                <strong style={{ fontFamily: "'Outfit', monospace", fontSize: '1.25rem', color: 'var(--primary-600)' }}>
                  {booking?.referenceCode}
                </strong>
              </div>
              <div className={styles.confirmRow}>
                <span>Status</span>
                <span className="badge badge-warning">PENDING VERIFICATION</span>
              </div>
            </div>
            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center', marginTop: '1.25rem', flexWrap: 'wrap' }}>
              <button 
                onClick={() => router.push(`/ticket/${booking?.pnr || booking?.referenceCode || bookingId || ''}`)} 
                className="btn btn-primary"
                style={{ gap: '0.5rem' }}
              >
                🎫 View e-Ticket & Live Status
              </button>
              <button 
                onClick={() => router.push('/profile')} 
                className="btn btn-outline"
              >
                All Bookings
              </button>
            </div>
          </div>
        </main>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <main className={styles.page}>
        <div className="container">
          <div className={styles.paymentGrid}>
            <div className={styles.qrSection}>
              {/* Fast Track / Instant Demo Checkout for Investors & QA */}
              <div style={{
                marginBottom: '1.25rem',
                padding: '1.25rem',
                background: 'linear-gradient(135deg, #042f2e 0%, #064e3b 100%)',
                borderRadius: '16px',
                color: '#ffffff',
                boxShadow: '0 10px 25px -5px rgba(13, 148, 136, 0.25)',
                border: '1px solid rgba(45, 212, 191, 0.3)',
              }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.65rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                  <span style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.35rem',
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    letterSpacing: '0.05em',
                    textTransform: 'uppercase',
                    background: 'rgba(255, 255, 255, 0.15)',
                    padding: '0.25rem 0.6rem',
                    borderRadius: '999px',
                    color: '#a7f3d0'
                  }}>
                    ⚡ Instant Demo Checkout
                  </span>
                  <span style={{ fontSize: '0.8rem', color: '#99f6e4' }}>
                    Zero delay • Instant QR Pass
                  </span>
                </div>

                <h3 style={{ fontSize: '1.15rem', fontWeight: 700, margin: '0 0 0.4rem', color: '#ffffff' }}>
                  Skip Gateway & Manual Verification
                </h3>
                <p style={{ fontSize: '0.85rem', color: '#ccfbf1', margin: '0 0 1rem', lineHeight: 1.5 }}>
                  Simulate instantaneous UPI bank authorization directly on Dhan-Gaadi Fleet OS to instantly unlock your confirmed e-ticket and verified QR boarding pass.
                </p>

                <button
                  type="button"
                  onClick={handleInstantDemoPayment}
                  disabled={instantDemoLoading || submitting || timeLeft <= 0}
                  className="btn btn-primary btn-lg btn-full"
                  style={{
                    background: '#10b981',
                    borderColor: '#10b981',
                    color: '#ffffff',
                    fontWeight: 700,
                    fontSize: '1rem',
                    padding: '0.875rem 1.25rem',
                    boxShadow: '0 4px 14px 0 rgba(16, 185, 129, 0.4)'
                  }}
                >
                  {instantDemoLoading ? (
                    <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                      <span className="spinner" style={{ width: 18, height: 18, borderWidth: 2 }} />
                      {instantDemoStep || 'Processing Instant Demo Payment...'}
                    </span>
                  ) : (
                    '⚡ Fast-Track Instant Pay (Demo Mode)'
                  )}
                </button>
              </div>

              {/* OR Divider */}
              <div style={{
                display: 'flex',
                alignItems: 'center',
                margin: '1.25rem 0',
                color: 'var(--text-tertiary)',
                fontSize: '0.75rem',
                textTransform: 'uppercase',
                letterSpacing: '0.05em'
              }}>
                <div style={{ flex: 1, height: '1px', background: 'var(--border-color)' }} />
                <span style={{ padding: '0 0.75rem', fontWeight: 600 }}>OR SCAN & UPLOAD RECEIPT</span>
                <div style={{ flex: 1, height: '1px', background: 'var(--border-color)' }} />
              </div>

              <div className={styles.timerBar} style={{ background: isUrgent ? 'var(--danger-light)' : 'var(--warning-light)' }}>
                <span>⏱️ Seat lock expires in</span>
                <strong style={{ color: isUrgent ? 'var(--danger)' : 'var(--warning)', fontFamily: "'Outfit', monospace", fontSize: '1.5rem' }}>
                  {formatTime(timeLeft)}
                </strong>
              </div>
              <div className={styles.qrCard}>
                <h2 className="heading-3" style={{ textAlign: 'center', marginBottom: '0.5rem' }}>Scan & Pay</h2>
                <p className="text-small" style={{ textAlign: 'center', color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
                  {qrCode?.upiLabel || 'Dhan-Gaadi Official UPI Pay'}
                </p>
                {qrCode?.imagePath ? (
                  <div className={styles.qrImage}>
                    <img src={buildAssetUrl(qrCode.imagePath)} alt="UPI QR Code" />
                  </div>
                ) : (
                  <div className={styles.qrPlaceholder}><span>QR code loading...</span></div>
                )}
                
                {/* Direct UPI Deep-link for mobile users */}
                {qrCode?.upiId && (
                  <div style={{ margin: '1rem 0 0.5rem', textAlign: 'center' }}>
                    <a
                      href={`upi://pay?pa=${encodeURIComponent(qrCode.upiId)}&pn=Dhan%20Gaadi%20Fleet&am=${booking?.totalFare || 0}&tn=${encodeURIComponent(booking?.referenceCode || 'BusTicket')}&cu=INR`}
                      className="btn btn-accent btn-md btn-full"
                      style={{ fontSize: '0.9375rem', gap: '0.5rem' }}
                    >
                      ⚡ Pay via UPI App (GPay / PhonePe)
                    </a>
                  </div>
                )}

                <div className={styles.amountBox}>
                  <span>Amount to Pay</span>
                  <strong>₹{booking?.totalFare}</strong>
                </div>

                {qrCode?.upiId && (
                  <div className={styles.refCode} style={{ cursor: 'pointer' }} onClick={() => {
                    navigator.clipboard?.writeText(qrCode.upiId);
                    setCopiedUpi(true);
                    if (upiTimerRef.current) clearTimeout(upiTimerRef.current);
                    upiTimerRef.current = setTimeout(() => setCopiedUpi(false), 2000);
                  }}>
                    <span>UPI VPA (Tap to copy):</span>
                    <div className={styles.refCodeValue} style={{ fontSize: '0.95rem' }}>
                      {qrCode.upiId} {copiedUpi ? '✅ Copied!' : '📋'}
                    </div>
                  </div>
                )}

                <div className={styles.refCode}>
                  <span>Include this in payment remarks:</span>
                  <div className={styles.refCodeValue}>{booking?.referenceCode}</div>
                </div>
              </div>

              {/* Enhanced Dropzone with Preview */}
              <div className={styles.uploadSection}>
                <input 
                  type="file" 
                  ref={fileInputRef} 
                  accept="image/*" 
                  onChange={(e) => {
                    if (e.target.files && e.target.files[0]) {
                      setScreenshot(e.target.files[0]);
                    }
                  }} 
                  style={{ display: 'none' }} 
                />

                {screenshot ? (
                  <div style={{ padding: '1rem', background: 'var(--bg-secondary)', borderRadius: '12px', border: '1.5px solid #a7f3d0', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', overflow: 'hidden' }}>
                      <span style={{ fontSize: '1.5rem' }}>🖼️</span>
                      <div style={{ overflow: 'hidden' }}>
                        <strong style={{ fontSize: '0.875rem', display: 'block', textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>
                          {screenshot.name}
                        </strong>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)' }}>
                          {(screenshot.size / 1024).toFixed(1)} KB • Ready to upload
                        </span>
                      </div>
                    </div>
                    <button 
                      type="button"
                      onClick={() => setScreenshot(null)} 
                      className="btn btn-ghost btn-sm"
                      style={{ color: 'var(--danger)', flexShrink: 0 }}
                    >
                      Remove
                    </button>
                  </div>
                ) : (
                  <button 
                    type="button"
                    className="btn btn-outline btn-full" 
                    onClick={() => fileInputRef.current?.click()}
                    style={{ padding: '0.875rem 1rem', borderStyle: 'dashed' }}
                  >
                    📎 Upload Payment Screenshot (Recommended)
                  </button>
                )}
              </div>

              {error && <div className={styles.errorBanner}>❌ {error}</div>}
              <button 
                className="btn btn-primary btn-lg btn-full" 
                onClick={handleSubmitPayment} 
                disabled={submitting || timeLeft <= 0}
                style={{ fontSize: '1.0625rem' }}
              >
                {submitting ? 'Submitting Receipt...' : '✓ I Have Completed the Payment'}
              </button>
            </div>
            <div className={styles.summarySection}>
              <div className={styles.summaryCard}>
                <h3 className="heading-4">Booking Details</h3>
                <div className={styles.summaryRow}><span>PNR</span><strong>{booking?.pnr}</strong></div>
                <div className={styles.summaryRow}><span>Route</span><span>{booking?.schedule?.route?.source} → {booking?.schedule?.route?.destination}</span></div>
                <div className={styles.summaryRow}><span>Seats</span><strong>{booking?.seats?.join(', ')}</strong></div>
                <div className={styles.summaryRow}><span>Passenger</span><span>{booking?.passengerName}</span></div>
                <div className={`${styles.summaryRow} ${styles.summaryTotal}`}>
                  <strong>Total</strong>
                  <strong style={{ color: 'var(--primary-600)', fontFamily: "'Outfit', sans-serif", fontSize: '1.5rem' }}>₹{booking?.totalFare}</strong>
                </div>
              </div>
              <div className={styles.instructions}>
                <h4 className="heading-4">Payment Instructions</h4>
                <ol>
                  <li>Scan the QR code with any UPI app</li>
                  <li>Enter the exact amount: <strong>₹{booking?.totalFare}</strong></li>
                  <li>Add reference <strong>{booking?.referenceCode}</strong> in remarks</li>
                  <li>Complete the payment</li>
                  <li>Upload screenshot (optional) and click &quot;I Have Made the Payment&quot;</li>
                  <li>Wait for admin verification (you&apos;ll receive SMS)</li>
                </ol>
              </div>
            </div>
          </div>
        </div>
      </main>
    </>
  );
}

export default function PaymentPage({ routeBookingId }) {
  return (
    <Suspense fallback={<div className="loading-overlay" style={{ minHeight: '100vh' }}><div className="spinner" /></div>}>
      <PaymentContent explicitBookingId={routeBookingId} />
    </Suspense>
  );
}
