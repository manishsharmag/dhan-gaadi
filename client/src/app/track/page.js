'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import Navbar from '@/components/Navbar';
import Footer from '@/components/Footer';
import { trackingAPI } from '@/lib/api';
import styles from './[scheduleId]/track.module.css';

export default function AllActiveTracksPage() {
  const [activeBuses, setActiveBuses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    async function fetchActive() {
      try {
        const res = await trackingAPI.getActiveBuses();
        const data = res.data;
        if (Array.isArray(data)) {
          setActiveBuses(data);
        } else {
          setActiveBuses([]);
        }
      } catch (err) {
        setError('Failed to fetch active bus fleet');
      } finally {
        setLoading(false);
      }
    }
    fetchActive();
    const interval = setInterval(fetchActive, 15000);
    return () => clearInterval(interval);
  }, []);

  const filteredBuses = activeBuses.filter(track => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    const busNo = track.schedule?.bus?.busNumber?.toLowerCase() || '';
    const src = track.schedule?.route?.source?.toLowerCase() || '';
    const dst = track.schedule?.route?.destination?.toLowerCase() || '';
    const routeNo = track.schedule?.route?.routeNumber?.toLowerCase() || '';
    return busNo.includes(q) || src.includes(q) || dst.includes(q) || routeNo.includes(q);
  });

  return (
    <>
      <Navbar />
      <main className={styles.page}>
        <div className="container">
          <div className={styles.header}>
            <div>
              <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.375rem', padding: '0.25rem 0.75rem', background: '#ecfdf5', borderRadius: '9999px', border: '1px solid #a7f3d0', color: '#065f46', fontSize: '0.75rem', fontWeight: 700, marginBottom: '0.5rem' }}>
                <span className={styles.dot} /> Live Fleet Operations
              </div>
              <h1 className="heading-2">📡 Live Highway Radar</h1>
              <p style={{ color: 'var(--text-secondary)', marginTop: '4px', fontSize: '0.9375rem' }}>
                Real-time GPS telemetry from conductor transmitters across Haryana State highways.
              </p>
            </div>
            
            <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
              <input
                type="text"
                className="form-input"
                placeholder="🔍 Search bus no., route, or city..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                style={{ maxWidth: '280px', padding: '0.5rem 0.875rem', fontSize: '0.875rem' }}
              />
              <Link href="/search" className="btn btn-secondary btn-sm">
                Book Seats
              </Link>
            </div>
          </div>

          {loading ? (
            <div className="loading-overlay"><div className="spinner" /></div>
          ) : activeBuses.length === 0 ? (
            <div className={styles.errorCard} style={{ background: 'var(--bg-card)', padding: '3.5rem 2rem', borderRadius: '24px', border: '1px solid var(--border-color)', textAlign: 'center', maxWidth: '600px', margin: '2rem auto' }}>
              <span style={{ fontSize: '3.5rem', display: 'block', marginBottom: '1rem' }}>🚌</span>
              <h3 className="heading-3" style={{ marginBottom: '0.5rem' }}>No Active Trips Broadcasting Right Now</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9375rem', marginBottom: '1.5rem', lineHeight: '1.6' }}>
                Live GPS telemetry streams when conductors initiate scheduled trips from their handheld mobile app. You can still search upcoming bus schedules or check your e-ticket.
              </p>
              <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
                <Link href="/search" className="btn btn-primary">
                  Search All Schedules →
                </Link>
                <Link href="/ticket" className="btn btn-secondary">
                  Find e-Ticket
                </Link>
              </div>
            </div>
          ) : (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '1.5rem 0 1rem' }}>
                <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                  Showing {filteredBuses.length} active coaches on highway
                </span>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)' }}>
                  Auto-updates every 15s
                </span>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.25rem' }}>
                {filteredBuses.map((track) => {
                  const schedule = track.schedule;
                  const route = schedule?.route;
                  const bus = schedule?.bus;
                  const scheduleId = schedule?._id || track.schedule;

                  return (
                    <div 
                      key={track._id} 
                      className="card" 
                      style={{ 
                        padding: '1.5rem', 
                        borderRadius: '20px', 
                        border: '1px solid var(--border-color)', 
                        background: 'var(--bg-card)', 
                        display: 'flex', 
                        flexDirection: 'column', 
                        justifyContent: 'space-between',
                        boxShadow: 'var(--shadow-md)',
                        transition: 'transform 0.2s ease, box-shadow 0.2s ease'
                      }}
                    >
                      <div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                          <div>
                            <span style={{ fontWeight: '800', fontSize: '1.15rem', color: 'var(--primary-700)', fontFamily: 'Outfit, sans-serif' }}>
                              {bus?.busNumber || 'HR Bus'}
                            </span>
                            <span style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginLeft: '0.5rem' }}>
                              {bus?.type?.toUpperCase() || 'EXPRESS'}
                            </span>
                          </div>
                          <span className={`${styles.statusBadge} ${styles.active}`} style={{ fontSize: '0.75rem', padding: '0.25rem 0.65rem' }}>
                            <span className={styles.dot} /> LIVE
                          </span>
                        </div>

                        <div style={{ fontSize: '1.05rem', fontWeight: '700', marginBottom: '0.75rem', color: 'var(--text-primary)' }}>
                          {route ? `${route.source} ➔ ${route.destination}` : 'Haryana Highway Route'}
                        </div>

                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem', padding: '0.75rem', background: 'var(--bg-secondary)', borderRadius: '12px', marginBottom: '1.25rem', fontSize: '0.8125rem' }}>
                          <div>
                            <span style={{ color: 'var(--text-tertiary)', display: 'block' }}>Speed</span>
                            <strong style={{ color: '#008574', fontSize: '0.95rem' }}>{track.speed || 0} km/h</strong>
                          </div>
                          <div>
                            <span style={{ color: 'var(--text-tertiary)', display: 'block' }}>Departure</span>
                            <strong style={{ color: 'var(--text-primary)', fontSize: '0.95rem' }}>{schedule?.departureTime || '--:--'}</strong>
                          </div>
                        </div>
                      </div>

                      <Link href={`/track/${scheduleId}`} className="btn btn-primary btn-full" style={{ textAlign: 'center' }}>
                        📍 Open Live GPS Radar →
                      </Link>
                    </div>
                  );
                })}
              </div>
            </>
          )}
        </div>
      </main>
      <Footer />
    </>
  );
}
