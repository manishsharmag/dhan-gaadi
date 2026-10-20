'use client';
import { useState, useEffect, useCallback, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import Navbar from '@/components/Navbar';
import Footer from '@/components/Footer';
import { scheduleAPI } from '@/lib/api';
import styles from './search.module.css';

function SearchContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const source = searchParams.get('source') || '';
  const destination = searchParams.get('destination') || '';
  const date = searchParams.get('date') || '';
  
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(() => Boolean(source && destination));
  const [error, setError] = useState('');
  const [selectedType, setSelectedType] = useState('all');

  const [inputSource, setInputSource] = useState(source);
  const [inputDestination, setInputDestination] = useState(destination);
  const [inputDate, setInputDate] = useState(() => date || new Date().toISOString().split('T')[0]);

  const fetchSchedules = useCallback(async () => {
    if (!source || !destination) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    try {
      const res = await scheduleAPI.search({ source, destination, date });
      setSchedules(res.data || []);
    } catch (err) {
      setError(err.message || 'Failed to fetch schedules');
      setSchedules([]);
    } finally {
      setLoading(false);
    }
  }, [source, destination, date]);

  useEffect(() => {
    if (source && destination) {
      fetchSchedules();
    } else {
      setLoading(false);
    }
  }, [source, destination, fetchSchedules]);

  const handleManualSearch = (e) => {
    e.preventDefault();
    if (!inputSource.trim() || !inputDestination.trim()) return;
    const params = new URLSearchParams({
      source: inputSource.trim(),
      destination: inputDestination.trim(),
    });
    if (inputDate) params.set('date', inputDate);
    router.push(`/search?${params.toString()}`);
  };

  const handleSelectSchedule = (scheduleId) => {
    const params = new URLSearchParams();
    if (date) params.set('date', date);
    router.push(`/booking?scheduleId=${scheduleId}&${params.toString()}`);
  };

  const getBusTypeLabel = (type) => {
    const labels = {
      'ordinary': 'Ordinary',
      'deluxe': 'Deluxe',
      'volvo-ac': 'Volvo AC',
      'super-deluxe': 'Super Deluxe',
    };
    return labels[type] || type;
  };

  const getBusTypeClass = (type) => {
    if (type === 'volvo-ac') return styles.typeAC;
    if (type === 'deluxe' || type === 'super-deluxe') return styles.typeDeluxe;
    return styles.typeOrdinary;
  };

  const filteredSchedules = schedules.filter(s => {
    if (selectedType === 'all') return true;
    return s.bus?.type === selectedType;
  });

  return (
    <>
      <Navbar />
      <main className={styles.page}>
        <div className="container">
          {!source || !destination ? (
            <div className="card" style={{ maxWidth: 600, margin: '3rem auto', padding: '2.5rem' }}>
              <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
                <span style={{ fontSize: '2.5rem' }}>🚌</span>
                <h2 className="heading-2" style={{ marginTop: '0.5rem' }}>Search Dhan-Gaadi Fleet Buses</h2>
                <p style={{ color: 'var(--text-secondary)' }}>Enter departure and arrival points to view schedules and book seats.</p>
              </div>
              <form onSubmit={handleManualSearch} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                <div className="form-group">
                  <label className="form-label">From (Departure City/Stop)</label>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="e.g. Hisar, Delhi, Ambala"
                    value={inputSource}
                    onChange={(e) => setInputSource(e.target.value)}
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">To (Arrival City/Stop)</label>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="e.g. Chandigarh, Rohtak, Gurugram"
                    value={inputDestination}
                    onChange={(e) => setInputDestination(e.target.value)}
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Date of Journey</label>
                  <input
                    type="date"
                    className="form-input"
                    value={inputDate}
                    onChange={(e) => setInputDate(e.target.value)}
                  />
                </div>
                <button type="submit" className="btn btn-primary" style={{ width: '100%', padding: '0.85rem' }}>
                  🔍 Find Available Buses
                </button>
              </form>
            </div>
          ) : (
            <>
              {/* Search header */}
              <div className={styles.searchHeader}>
                <div className={styles.searchInfo}>
                  <div className={styles.routeHeaderBadge}>
                    <span>🛣️</span> Official Route Service
                  </div>
                  <h1 className="heading-2">
                    {source} ➔ {destination}
                  </h1>
                  <p className="text-body" style={{ color: 'var(--text-secondary)' }}>
                    {date ? new Date(date).toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }) : 'All operating departures'}
                  </p>
                </div>
                <button onClick={() => router.push('/')} className="btn btn-secondary">
                  ← Modify Search
                </button>
              </div>

          {/* Bus Type Filter Tabs */}
          {schedules.length > 0 && (
            <div className={styles.filterBar}>
              <button
                className={`${styles.filterChip} ${selectedType === 'all' ? styles.activeFilter : ''}`}
                onClick={() => setSelectedType('all')}
              >
                All Coaches ({schedules.length})
              </button>
              <button
                className={`${styles.filterChip} ${selectedType === 'volvo-ac' ? styles.activeFilter : ''}`}
                onClick={() => setSelectedType('volvo-ac')}
              >
                ❄️ Volvo AC
              </button>
              <button
                className={`${styles.filterChip} ${selectedType === 'deluxe' || selectedType === 'super-deluxe' ? styles.activeFilter : ''}`}
                onClick={() => setSelectedType('deluxe')}
              >
                ⭐ Deluxe Express
              </button>
              <button
                className={`${styles.filterChip} ${selectedType === 'ordinary' ? styles.activeFilter : ''}`}
                onClick={() => setSelectedType('ordinary')}
              >
                🚌 Ordinary
              </button>
            </div>
          )}

          {/* Results */}
          {loading ? (
            <div className="loading-overlay">
              <div className="spinner" />
            </div>
          ) : error ? (
            <div className={styles.errorState}>
              <span>⚠️</span>
              <p>{error}</p>
              <button onClick={fetchSchedules} className="btn btn-primary">Try Again</button>
            </div>
          ) : filteredSchedules.length === 0 ? (
            <div className="empty-state">
              <span className="empty-icon">🚌</span>
              <h3 className="heading-3">No matching buses found</h3>
              <p style={{ color: 'var(--text-secondary)' }}>Try selecting &quot;All Coaches&quot; or choosing another journey date.</p>
              <button onClick={() => setSelectedType('all')} className="btn btn-secondary" style={{ marginTop: '1rem' }}>
                Show All Coaches
              </button>
            </div>
          ) : (
            <div className={styles.resultsList}>
              <div className={styles.resultsMeta}>
                <span className="text-small" style={{ color: 'var(--text-secondary)', fontWeight: 600 }}>
                  Showing {filteredSchedules.length} departures
                </span>
                <span className={styles.liveTrackingPill}>● Live GPS Available</span>
              </div>
              {filteredSchedules.map((schedule) => (
                <div key={schedule._id} className={styles.scheduleCard}>
                  <div className={styles.scheduleTop}>
                    <div className={styles.scheduleRoute}>
                      <span className={`${styles.busType} ${getBusTypeClass(schedule.bus?.type)}`}>
                        {getBusTypeLabel(schedule.bus?.type)}
                      </span>
                      <span className={styles.routeNumber}>{schedule.route?.routeNumber || 'HR EXPRESS'}</span>
                    </div>
                    <div className={styles.scheduleFare}>
                      <span className={styles.fareLabel}>Starting from</span>
                      <span className={styles.fareAmount}>₹{schedule.fare || schedule.route?.baseFare || 0}</span>
                    </div>
                  </div>
                  
                  <div className={styles.scheduleBody}>
                    <div className={styles.timeBlock}>
                      <span className={styles.time}>{schedule.departureTime}</span>
                      <span className={styles.city}>{schedule.route?.source}</span>
                      <span className={styles.depot}>{schedule.route?.sourceDepot || 'Main Bus Stand'}</span>
                    </div>
                    
                    <div className={styles.journey}>
                      <div className={styles.journeyLine}>
                        <span className={styles.dot} />
                        <span className={styles.line} />
                        <span className={styles.dot} />
                      </div>
                      <span className={styles.distance}>{schedule.route?.totalDistance || 150} km direct</span>
                    </div>
                    
                    <div className={styles.timeBlock}>
                      <span className={styles.time}>{schedule.arrivalTime || '—'}</span>
                      <span className={styles.city}>{schedule.route?.destination}</span>
                      <span className={styles.depot}>{schedule.route?.destinationDepot || 'ISBT Depot'}</span>
                    </div>
                  </div>
                  
                  <div className={styles.scheduleBottom}>
                    <div className={styles.amenitiesRow}>
                      <span className={styles.amenityBadge}>🚌 {schedule.bus?.busNumber || 'HR-38-XXXX'}</span>
                      <span className={styles.amenityBadge}>💺 {schedule.bus?.totalSeats || 45} Seats</span>
                      <span className={styles.amenityBadge}>⚡ Mobile Charging</span>
                      <span className={styles.amenityBadge}>📍 Live Radar</span>
                    </div>
                    <button 
                      className="btn btn-primary btn-md"
                      onClick={() => handleSelectSchedule(schedule._id)}
                    >
                      Select Seats →
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
      </main>
      <Footer />
    </>
  );
}

export default function SearchPage() {
  return (
    <Suspense fallback={<div className="loading-overlay"><div className="spinner" /></div>}>
      <SearchContent />
    </Suspense>
  );
}
