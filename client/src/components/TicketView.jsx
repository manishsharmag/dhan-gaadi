'use client';
import { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import QRCode from 'qrcode';
import { buildTicketPdfUrl } from '@/lib/config';
import { bookingAPI } from '@/lib/api';
import styles from '@/app/ticket/ticket.module.css';

export default function TicketView({ booking }) {
  const [copied, setCopied] = useState(false);
  const [qrDataUrl, setQrDataUrl] = useState('');
  const [currentStatus, setCurrentStatus] = useState(booking?.status || 'CONFIRMED');
  const [cancelling, setCancelling] = useState(false);
  const [cancelMessage, setCancelMessage] = useState('');
  const copyTimerRef = useRef(null);

  useEffect(() => {
    return () => {
      if (copyTimerRef.current) clearTimeout(copyTimerRef.current);
    };
  }, []);

  useEffect(() => {
    if (booking?.status) {
      setCurrentStatus(booking.status);
    }
  }, [booking?.status]);

  useEffect(() => {
    if (booking?.pnr) {
      const origin = typeof window !== 'undefined' ? window.location.origin : 'https://haryanabusinfo.in';
      const ticketUrl = `${origin}/ticket?pnr=${booking.pnr}`;
      QRCode.toDataURL(ticketUrl, {
        width: 180,
        margin: 1,
        color: { dark: '#0F172A', light: '#FFFFFF' },
      })
        .then((url) => setQrDataUrl(url))
        .catch((err) => console.error('Failed to generate QR code', err));
    }
  }, [booking?.pnr]);

  if (!booking) return null;

  const source = booking.schedule?.route?.source || 'SRC';
  const destination = booking.schedule?.route?.destination || 'DST';
  const boardingPoint = booking.boardingPoint || source;
  const droppingPoint = booking.droppingPoint || destination;
  const formattedDate = booking.journeyDate
    ? new Date(booking.journeyDate).toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
      })
    : '--';

  const handleCopyPnr = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(booking.pnr);
      setCopied(true);
      if (copyTimerRef.current) clearTimeout(copyTimerRef.current);
      copyTimerRef.current = setTimeout(() => setCopied(false), 2000);
    }
  };

  const handlePrint = () => {
    if (typeof window !== 'undefined') {
      window.print();
    }
  };

  const handleCancelBooking = async () => {
    if (!window.confirm('Are you sure you want to cancel this booking? The locked/reserved seats will be released immediately.')) {
      return;
    }
    setCancelling(true);
    try {
      await bookingAPI.cancel(booking._id || booking.id);
      setCurrentStatus('CANCELLED');
      setCancelMessage('This booking has been cancelled. Reserved seats have been released back to inventory.');
    } catch (err) {
      alert('Failed to cancel booking: ' + (err.message || 'Unknown error'));
    } finally {
      setCancelling(false);
    }
  };

  const handleShare = () => {
    if (navigator.share) {
      navigator.share({
        title: 'Dhan-Gaadi Fleet OS e-Ticket',
        text: `Dhan-Gaadi Boarding Pass\nPNR: ${booking.pnr}\nPassenger: ${booking.passengerName}\nRoute: ${source} to ${destination}\nBoarding: ${boardingPoint}\nDate: ${formattedDate}\nSeats: ${booking.seats?.join(', ')}`,
        url: window.location.href,
      }).catch(() => {});
    } else {
      handleCopyPnr();
    }
  };

  const scheduleId = booking.schedule?._id || booking.schedule;
  const canCancel = ['CONFIRMED', 'PENDING_VERIFICATION', 'SEAT_LOCKED'].includes(currentStatus);

  return (
    <div className={styles.ticketWrapper}>
      <div className={styles.ticket}>
        {/* Top Gradient Header */}
        <div className={styles.ticketHeader} style={{ background: 'linear-gradient(135deg, #0f766e 0%, #064e3b 100%)' }}>
          <div className={styles.brandRow}>
            <div className={styles.brandLogo}>
              <span>🚌</span>
              <div>
                <strong>DHAN-GAADI FLEET OS</strong>
                <span style={{ display: 'block', fontSize: '0.75rem', opacity: 0.85 }}>Enterprise Intercity Mobility</span>
              </div>
            </div>
            <span className={styles.statusBadge}>
              {currentStatus === 'CONFIRMED' ? '● CONFIRMED' : currentStatus}
            </span>
          </div>

          <div className={styles.routeRow}>
            <div className={styles.cityCol}>
              <span className={styles.cityCode}>{source.slice(0, 3).toUpperCase()}</span>
              <span className={styles.cityName}>{source}</span>
            </div>

            <div className={styles.busIconWrap}>
              <span>➔</span>
            </div>

            <div className={`${styles.cityCol} ${styles.right}`}>
              <span className={styles.cityCode}>{destination.slice(0, 3).toUpperCase()}</span>
              <span className={styles.cityName}>{destination}</span>
            </div>
          </div>
        </div>

        {/* Cancellation Message Banner */}
        {cancelMessage && (
          <div style={{
            margin: '1rem',
            padding: '0.85rem 1rem',
            background: '#FEF2F2',
            border: '1px solid #FECACA',
            borderRadius: '10px',
            color: '#991B1B',
            fontSize: '0.875rem'
          }}>
            <strong>✓ Booking Cancelled:</strong> {cancelMessage}
          </div>
        )}

        {/* Ticket Details */}
        <div className={styles.ticketBody}>
          <div className={styles.infoGrid}>
            <div className={styles.infoItem}>
              <span className={styles.infoLabel}>Primary Passenger</span>
              <strong className={styles.infoValue}>{booking.passengerName}</strong>
            </div>

            <div className={`${styles.infoItem} ${styles.right}`}>
              <span className={styles.infoLabel}>Assigned Seats</span>
              <strong className={`${styles.infoValue} ${styles.seatsBadge}`}>
                {[...(booking.seats || [])].sort((a, b) => a - b).join(', ') || '--'}
              </strong>
            </div>

            <div className={styles.infoItem}>
              <span className={styles.infoLabel}>Boarding Station</span>
              <strong className={styles.infoValue}>{boardingPoint}</strong>
            </div>

            <div className={`${styles.infoItem} ${styles.right}`}>
              <span className={styles.infoLabel}>Dropping Station</span>
              <strong className={styles.infoValue}>{droppingPoint}</strong>
            </div>

            <div className={styles.infoItem}>
              <span className={styles.infoLabel}>Journey Date</span>
              <strong className={styles.infoValue}>{formattedDate}</strong>
            </div>

            <div className={`${styles.infoItem} ${styles.right}`}>
              <span className={styles.infoLabel}>Departure Time</span>
              <strong className={styles.infoValue}>
                {booking.schedule?.departureTime || '--'}
              </strong>
            </div>

            <div className={styles.infoItem}>
              <span className={styles.infoLabel}>Bus Class</span>
              <strong className={styles.infoValue}>
                {booking.schedule?.bus?.type?.toUpperCase() || 'ORDINARY EXPRESS'}
              </strong>
            </div>

            <div className={`${styles.infoItem} ${styles.right}`}>
              <span className={styles.infoLabel}>Total Fare Paid</span>
              <strong className={`${styles.infoValue} ${styles.fareValue}`}>
                ₹{booking.totalFare}
              </strong>
            </div>
          </div>

          {/* Multi-Passenger manifest if present */}
          {Array.isArray(booking.passengers) && booking.passengers.length > 0 && (
            <div style={{ marginTop: '1rem', padding: '0.75rem 1rem', background: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
              <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em', display: 'block', marginBottom: '0.5rem' }}>
                👥 Passenger Seat Manifest
              </span>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.5rem' }}>
                {booking.passengers.map((p, idx) => (
                  <div key={idx} style={{ padding: '0.4rem 0.6rem', background: '#ffffff', borderRadius: '6px', border: '1px solid #e2e8f0', fontSize: '0.8125rem' }}>
                    <strong style={{ color: 'var(--primary-700)' }}>Seat {p.seatNumber}:</strong> {p.name}
                    {(p.age || p.gender) && (
                      <span style={{ color: '#64748b', marginLeft: '4px' }}>
                        ({[p.age ? `${p.age}y` : '', p.gender].filter(Boolean).join(', ')})
                      </span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Rejection Alert */}
          {booking.status === 'REJECTED' && (
            <div style={{
              margin: '1rem 0',
              padding: '1rem',
              background: '#FEF2F2',
              border: '1px solid #FECACA',
              borderRadius: '12px',
              color: '#991B1B'
            }}>
              <strong style={{ display: 'block', fontSize: '1rem', marginBottom: '0.25rem' }}>✕ Booking Rejected</strong>
              <p style={{ margin: 0, fontSize: '0.9rem', color: '#B91C1C' }}>
                Reason: {booking.rejectionReason || 'Payment or booking details could not be verified by Dhan-Gaadi Fleet staff.'}
              </p>
              <p style={{ margin: '0.5rem 0 0', fontSize: '0.75rem', color: '#6B7280' }}>
                If money was debited from your account, it will be refunded. Please contact Dhan-Gaadi helpline at 1800-180-2057 with your PNR.
              </p>
            </div>
          )}

          {/* Pending Verification Notice */}
          {booking.status === 'PENDING_VERIFICATION' && (
            <div style={{
              margin: '1rem 0',
              padding: '1rem',
              background: '#FFFBEB',
              border: '1px solid #FDE68A',
              borderRadius: '12px',
              color: '#92400E'
            }}>
              <strong style={{ display: 'block', fontSize: '1rem', marginBottom: '0.25rem' }}>⏳ Payment Verification In Progress</strong>
              <p style={{ margin: 0, fontSize: '0.875rem', color: '#B45309' }}>
                Your seats are locked. Dhan-Gaadi Fleet staff is currently verifying your payment receipt/UTR. Refresh this page to check live status.
              </p>
            </div>
          )}

          {/* Assigned Conductor Trust Card */}
          {booking.schedule?.conductor && (
            <div style={{
              margin: '1rem 0',
              padding: '1rem',
              background: '#F0FDFA',
              border: '1px solid #CCFBF1',
              borderRadius: '12px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: '1rem',
              flexWrap: 'wrap'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <div style={{
                  width: '44px', height: '44px', borderRadius: '50%',
                  background: 'var(--primary-100, #CCFBF1)', display: 'flex',
                  alignItems: 'center', justifyContent: 'center', fontSize: '1.4rem',
                  overflow: 'hidden', flexShrink: 0
                }}>
                  {booking.schedule.conductor.photo ? (
                    <img src={booking.schedule.conductor.photo} alt={booking.schedule.conductor.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  ) : '👮'}
                </div>
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--primary-900, #134E4A)' }}>
                    {booking.schedule.conductor.name}
                  </div>
                  <div style={{ fontSize: '0.8rem', color: '#0F766E' }}>
                    Assigned Bus Conductor • Dhan-Gaadi Fleet
                  </div>
                  <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--primary-700, #0F766E)', marginTop: '2px' }}>
                    📞 +91 {booking.schedule.conductor.mobile}
                  </div>
                </div>
              </div>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <a href={`tel:${booking.schedule.conductor.mobile}`} className="btn btn-outline btn-sm" style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }}>
                  📞 Call Staff
                </a>
                <a
                  href={`https://api.whatsapp.com/send?phone=91${booking.schedule.conductor.mobile.replace(/\D/g, '').slice(-10)}&text=${encodeURIComponent(`Hello Conductor ${booking.schedule.conductor.name}, I am a passenger on bus (${booking.schedule?.bus?.busNumber || ''}) with PNR ${booking.pnr}.`)}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="btn btn-sm"
                  style={{ background: '#25D366', color: '#fff', padding: '0.4rem 0.8rem', fontSize: '0.8rem' }}
                >
                  💬 WhatsApp
                </a>
              </div>
            </div>
          )}
        </div>

        {/* Perforated Divider */}
        <div className={styles.perforation}>
          <div className={styles.notchLeft} />
          <div className={styles.dashedLine} />
          <div className={styles.notchRight} />
        </div>

        {/* PNR & QR Code Verification */}
        <div className={styles.pnrSection}>
          <span className={styles.infoLabel}>BOARDING PASS PNR</span>
          <div className={styles.pnrBox} onClick={handleCopyPnr} title="Click to copy PNR">
            <span className={styles.pnrCode}>{booking.pnr}</span>
            <span style={{ fontSize: '0.85rem' }}>{copied ? '✅ Copied!' : '📋'}</span>
          </div>

          <div className={styles.qrContainer}>
            {qrDataUrl ? (
              <img src={qrDataUrl} alt="Boarding QR Code" className={styles.qrImg} width={180} height={180} />
            ) : (
              <div style={{ width: 180, height: 180, display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#F8FAFC', borderRadius: 8, fontSize: '0.85rem', color: '#64748B' }}>
                Generating QR...
              </div>
            )}
          </div>

          {/* Action Row */}
          <div className={styles.ticketActions}>
            {currentStatus === 'CONFIRMED' && (
              <a
                href={buildTicketPdfUrl(booking)}
                className={styles.btnPrimary}
                target="_blank"
                rel="noopener noreferrer"
              >
                📥 Download PDF Ticket
              </a>
            )}

            {scheduleId && (
              <Link href={`/track/${scheduleId}`} className={styles.btnOutlined}>
                📍 Live GPS Radar
              </Link>
            )}

            <button
              onClick={() => {
                const text = `🚌 *DHAN-GAADI FLEET OS e-TICKET*\n━━━━━━━━━━━━━━━━━━━━\n🎫 *PNR:* ${booking.pnr}\n👤 *Passenger:* ${booking.passengerName}\n📍 *Route:* ${source} ➔ ${destination}\n🚏 *Boarding:* ${boardingPoint}\n📅 *Date:* ${formattedDate} (${booking.schedule?.departureTime || ''})\n💺 *Seats:* ${booking.seats?.join(', ')}\n💵 *Fare Paid:* ₹${booking.totalFare}\n📲 *View Official Ticket:* ${window.location.href}\n━━━━━━━━━━━━━━━━━━━━\n_Dhan-Gaadi Fleet OS Mobility Portal_`;
                window.open(`https://api.whatsapp.com/send?text=${encodeURIComponent(text)}`, '_blank');
              }}
              className={styles.btnOutlined}
              style={{ color: '#059669', borderColor: '#059669' }}
            >
              💬 WhatsApp
            </button>

            <button onClick={handlePrint} className={styles.btnOutlined}>
              🖨️ Print Pass
            </button>

            <button onClick={handleShare} className={styles.btnOutlined}>
              🔗 Copy Link
            </button>

            {canCancel && (
              <button
                onClick={handleCancelBooking}
                disabled={cancelling}
                className={styles.btnOutlined}
                style={{ color: '#dc2626', borderColor: '#fca5a5' }}
              >
                {cancelling ? 'Cancelling...' : '✕ Cancel Booking'}
              </button>
            )}
          </div>

          <div style={{ margin: '1.25rem 0 0.5rem', padding: '0.75rem 1rem', background: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0', width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.75rem', color: '#64748b' }}>
            <span>📞 Dhan-Gaadi Helpline: <strong>1800-180-2057</strong></span>
            <span>🛡️ Verified Digital Fleet Pass</span>
          </div>

          <p className={styles.ticketFooter}>
            Official Dhan-Gaadi Fleet OS digital boarding pass. Present this QR code or PNR to the conductor during boarding.
          </p>
        </div>
      </div>
    </div>
  );
}
