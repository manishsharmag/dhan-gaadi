'use client';
import { useState, useEffect, useCallback, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import Navbar from '@/components/Navbar';
import Footer from '@/components/Footer';
import TicketView from '@/components/TicketView';
import { bookingAPI } from '@/lib/api';
import styles from './ticket.module.css';

function TicketContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pnr = searchParams.get('pnr');
  const [pnrInput, setPnrInput] = useState('');
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(() => Boolean(pnr));
  const [error, setError] = useState('');

  const fetchTicket = useCallback(async (searchPnr) => {
    const targetPnr = searchPnr || pnr;
    if (!targetPnr) {
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      setError('');
      const res = await bookingAPI.getByPNR(targetPnr);
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
  }, [pnr]);

  useEffect(() => {
    if (pnr) {
      fetchTicket(pnr);
    } else {
      setLoading(false);
    }
  }, [pnr, fetchTicket]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    const cleanPnr = pnrInput.trim().toUpperCase();
    if (!cleanPnr) return;
    router.push(`/ticket/${encodeURIComponent(cleanPnr)}`);
  };

  if (loading) {
    return (
      <div className="loading-overlay" style={{ minHeight: '70vh' }}>
        <div className="spinner" />
      </div>
    );
  }

  if (!pnr) {
    return (
      <main className={styles.page}>
        <div className="container" style={{ maxWidth: 560, margin: '3rem auto' }}>
          <div className="card" style={{ padding: '2.5rem', textAlign: 'center' }}>
            <span style={{ fontSize: '3rem' }}>🎫</span>
            <h1 className="heading-2" style={{ marginTop: '0.75rem', marginBottom: '0.5rem' }}>Find Your e-Ticket</h1>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '1.75rem' }}>
              Enter your PNR or Reference Code to view, download, or print your bus ticket.
            </p>
            <form onSubmit={handleSearchSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <input
                type="text"
                className="form-input"
                placeholder="Enter PNR (e.g. HR89A1B2) or Ref Code (e.g. HRB-XXXXXX)"
                value={pnrInput}
                onChange={(e) => setPnrInput(e.target.value.toUpperCase())}
                style={{ textAlign: 'center', fontSize: '1.1rem', letterSpacing: '1px', fontWeight: 600 }}
                required
              />
              <button type="submit" className="btn btn-primary" style={{ padding: '0.85rem' }}>
                🔍 Retrieve Ticket
              </button>
            </form>
          </div>
        </div>
      </main>
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
          <button onClick={() => router.push('/ticket')} className="btn btn-secondary">
            🔍 Search Another PNR
          </button>
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

export default function TicketPage() {
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
        <TicketContent />
      </Suspense>
      <Footer />
    </>
  );
}
