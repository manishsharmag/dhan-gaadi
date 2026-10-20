'use client';
import { useState, useEffect, useCallback, use, Suspense } from 'react';
import Link from 'next/link';
import Navbar from '@/components/Navbar';
import Footer from '@/components/Footer';
import TicketView from '@/components/TicketView';
import { bookingAPI } from '@/lib/api';
import styles from '@/app/ticket/ticket.module.css';

function TicketDetailContent({ params }) {
  const unwrappedParams = typeof params?.then === 'function' ? use(params) : params;
  const rawPnr = unwrappedParams?.pnr;
  const pnr = rawPnr ? decodeURIComponent(rawPnr).trim() : '';

  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(() => Boolean(pnr));
  const [error, setError] = useState('');

  const fetchTicket = useCallback(async (targetPnr) => {
    if (!targetPnr) {
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      setError('');
      const res = await bookingAPI.getByPNR(targetPnr);
      // Support unwrapped data, axios payload, or direct object
      const ticketData = res?.data?.pnr || res?.data?._id || res?.data?.referenceCode
        ? res.data
        : (res?.pnr || res?._id || res?.referenceCode ? res : (res?.data || res));

      if (ticketData && (ticketData.pnr || ticketData._id || ticketData.referenceCode)) {
        setBooking(ticketData);
      } else {
        setError(res?.message || 'Ticket not found');
      }
    } catch (err) {
      setError(err?.message || 'Failed to load ticket');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (pnr) {
      fetchTicket(pnr);
    } else {
      setLoading(false);
    }
  }, [pnr, fetchTicket]);

  if (loading) {
    return (
      <div className="loading-overlay" style={{ minHeight: '70vh' }}>
        <div className="spinner" />
      </div>
    );
  }

  if (error || !booking) {
    return (
      <div className="empty-state" style={{ minHeight: '60vh', padding: '3rem 1rem' }}>
        <span className="empty-icon">🎫</span>
        <h2 className="heading-2">Ticket Not Found</h2>
        <p style={{ color: 'var(--text-secondary)', margin: '0.5rem 0 1.5rem', maxWidth: 460 }}>
          {error || `We could not find an e-Ticket matching "${pnr}". Please check your PNR or Reference Code.`}
        </p>
        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
          <Link href="/ticket" className="btn btn-secondary">
            🔍 Search Another PNR
          </Link>
          <Link href="/profile" className="btn btn-outline">
            📋 My Bookings
          </Link>
          <Link href="/" className="btn btn-primary">
            🏠 Back to Home
          </Link>
        </div>
      </div>
    );
  }

  return (
    <main className={styles.page}>
      <TicketView booking={booking} />
    </main>
  );
}

export default function DynamicTicketPage({ params }) {
  return (
    <>
      <Navbar />
      <Suspense
        fallback={
          <div className="loading-overlay" style={{ minHeight: '70vh' }}>
            <div className="spinner" />
          </div>
        }
      >
        <TicketDetailContent params={params} />
      </Suspense>
      <Footer />
    </>
  );
}
