'use client';
import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Navbar from '@/components/Navbar';
import Footer from '@/components/Footer';
import { routeAPI } from '@/lib/api';
import styles from './page.module.css';

// Default Haryana cities for dropdowns fallback
const DEFAULT_CITIES = [
  'Hisar', 'Chandigarh', 'Delhi', 'Rohtak', 'Ambala', 
  'Karnal', 'Panipat', 'Jind', 'Kurukshetra', 'Sonipat',
  'Bahadurgarh', 'Hansi', 'Gurugram', 'Faridabad', 'Rewari'
];

const POPULAR_ROUTES = [
  { from: 'Hisar', to: 'Chandigarh', price: '₹450', duration: '5h 30m', routeNo: 'HR-38' },
  { from: 'Hisar', to: 'Delhi', price: '₹320', duration: '4h', routeNo: 'HR-72' },
  { from: 'Ambala', to: 'Delhi', price: '₹380', duration: '4h', routeNo: 'HR-44' },
  { from: 'Rohtak', to: 'Chandigarh', price: '₹420', duration: '5h 30m', routeNo: 'HR-58' },
  { from: 'Hisar', to: 'Rohtak', price: '₹200', duration: '2h 30m', routeNo: 'HR-16' },
];

export default function HomePage() {
  const router = useRouter();
  const [cities, setCities] = useState(DEFAULT_CITIES);
  const [source, setSource] = useState('');
  const [destination, setDestination] = useState('');
  const [dates] = useState(() => {
    const now = new Date();
    const today = now.toISOString().split('T')[0];
    const tomorrow = new Date(now.getTime() + 86400000).toISOString().split('T')[0];
    return { today, tomorrow };
  });
  const todayDate = dates.today;
  const tomorrowDate = dates.tomorrow;
  const [date, setDate] = useState(dates.today);
  const [searchMode, setSearchMode] = useState('book'); // 'book', 'track', 'pnr'
  const [pnrInput, setPnrInput] = useState('');

  useEffect(() => {
    routeAPI.getCities()
      .then((res) => {
        if (Array.isArray(res.data) && res.data.length > 0) {
          setCities(res.data);
        }
      })
      .catch(() => {
        // Keep fallback
      });
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchMode === 'pnr') {
      if (!pnrInput.trim()) return;
      router.push(`/ticket/${pnrInput.trim().toUpperCase()}`);
      return;
    }
    if (searchMode === 'track') {
      router.push('/track');
      return;
    }
    if (!source || !destination) return;
    const params = new URLSearchParams({ source, destination });
    if (date) params.set('date', date);
    router.push(`/search?${params.toString()}`);
  };

  const handlePopularRoute = (from, to) => {
    router.push(`/search?source=${from}&destination=${to}`);
  };

  return (
    <>
      <Navbar />
      <main>
        {/* Hero Section */}
        <section className={styles.hero}>
          <div className={styles.heroBg}>
            <div className={styles.heroPattern} />
            <div className={styles.heroGlow} />
          </div>
          <div className={`container ${styles.heroContent}`}>
            <div className={styles.heroText}>
              <div className={styles.heroBadge}>
                <span>🚀</span> Dhan-Gaadi Fleet OS • Enterprise Intercity Mobility
              </div>
              <h1 className={`heading-display ${styles.heroTitle}`}>
                Next-Generation
                <span className={styles.heroAccent}> Ground Mobility</span>
                <br />For Highway Transit
              </h1>
              <p className={styles.heroDesc}>
                Official enterprise bus reservation platform, real-time GPS telemetry radar, and instant verified QR boarding passes for high-frequency highway routes.
              </p>
              <div className={styles.heroStats}>
                <div className={styles.statItem}>
                  <strong>15+</strong>
                  <span>Active Routes</span>
                </div>
                <div className={styles.statDivider} />
                <div className={styles.statItem}>
                  <strong>Instant QR</strong>
                  <span>Fast Demo Checkout</span>
                </div>
                <div className={styles.statDivider} />
                <div className={styles.statItem}>
                  <strong>Live GPS</strong>
                  <span>Telemetry Radar</span>
                </div>
              </div>
            </div>

            {/* Interactive Search Card */}
            <div className={styles.searchCard}>
              <div className={styles.searchTabs}>
                <button
                  type="button"
                  className={`${styles.tabBtn} ${searchMode === 'book' ? styles.activeTab : ''}`}
                  onClick={() => setSearchMode('book')}
                >
                  <span>🚌</span> <span>Book Ticket</span>
                </button>
                <button
                  type="button"
                  className={`${styles.tabBtn} ${searchMode === 'track' ? styles.activeTab : ''}`}
                  onClick={() => setSearchMode('track')}
                >
                  <span>📍</span> <span>Live Tracking</span>
                </button>
                <button
                  type="button"
                  className={`${styles.tabBtn} ${searchMode === 'pnr' ? styles.activeTab : ''}`}
                  onClick={() => setSearchMode('pnr')}
                >
                  <span>🎫</span> <span>Find e-Ticket</span>
                </button>
              </div>

              {searchMode === 'pnr' ? (
                <form onSubmit={handleSearch} className={styles.searchForm}>
                  <div className={styles.searchField}>
                    <label className="form-label" htmlFor="pnr-input">Enter Booking PNR or Ref Code</label>
                    <input
                      id="pnr-input"
                      type="text"
                      className="form-input"
                      placeholder="e.g. HR-89A1B2 or HRB-K9X2"
                      value={pnrInput}
                      onChange={(e) => setPnrInput(e.target.value.toUpperCase())}
                      required
                    />
                  </div>
                  <button type="submit" className={`btn btn-primary btn-lg btn-full ${styles.searchBtn}`}>
                    Search Boarding Pass →
                  </button>
                </form>
              ) : searchMode === 'track' ? (
                <div className={styles.trackPromo}>
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.9375rem', marginBottom: '1.25rem' }}>
                    View real-time bus locations, current speed, route ETA, and next upcoming stops on Haryana highways.
                  </p>
                  <button
                    type="button"
                    onClick={() => router.push('/track')}
                    className={`btn btn-primary btn-lg btn-full ${styles.searchBtn}`}
                  >
                    Open Live Fleet Radar →
                  </button>
                </div>
              ) : (
                <form onSubmit={handleSearch} className={styles.searchForm}>
                  <div className={styles.searchField}>
                    <label className="form-label" htmlFor="search-source">Origin / Boarding Station</label>
                    <select 
                      className="form-select" 
                      value={source} 
                      onChange={(e) => setSource(e.target.value)}
                      required
                      id="search-source"
                    >
                      <option value="">Select departure city</option>
                      {cities.map(city => (
                        <option key={city} value={city} disabled={city === destination}>{city}</option>
                      ))}
                    </select>
                  </div>

                  {/* Swap button */}
                  <button 
                    type="button" 
                    className={styles.swapBtn}
                    onClick={() => { setSource(destination); setDestination(source); }}
                    aria-label="Swap departure and arrival cities"
                    title="Swap origin and destination"
                  >
                    ⇄
                  </button>

                  <div className={styles.searchField}>
                    <label className="form-label" htmlFor="search-destination">Destination / Dropping Station</label>
                    <select 
                      className="form-select" 
                      value={destination} 
                      onChange={(e) => setDestination(e.target.value)}
                      required
                      id="search-destination"
                    >
                      <option value="">Select arrival city</option>
                      {cities.map(city => (
                        <option key={city} value={city} disabled={city === source}>{city}</option>
                      ))}
                    </select>
                  </div>

                  <div className={styles.searchField}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <label className="form-label" htmlFor="search-date">Journey Date</label>
                      <div className={styles.dateChips}>
                        <button
                          type="button"
                          className={`${styles.dateChip} ${date === todayDate ? styles.activeDateChip : ''}`}
                          onClick={() => setDate(todayDate)}
                        >
                          Today
                        </button>
                        <button
                          type="button"
                          className={`${styles.dateChip} ${date === tomorrowDate ? styles.activeDateChip : ''}`}
                          onClick={() => setDate(tomorrowDate)}
                        >
                          Tomorrow
                        </button>
                      </div>
                    </div>
                    <input 
                      type="date" 
                      className="form-input" 
                      value={date} 
                      onChange={(e) => setDate(e.target.value)}
                      min={todayDate}
                      id="search-date"
                    />
                  </div>

                  <button type="submit" className={`btn btn-primary btn-lg btn-full ${styles.searchBtn}`} id="search-submit">
                    Search Available Coaches →
                  </button>
                </form>
              )}
            </div>
          </div>
        </section>

        {/* Popular Routes */}
        <section className={styles.routes}>
          <div className="container">
            <div className={styles.sectionHeader}>
              <h2 className="heading-2">Popular Routes</h2>
              <p className="text-body" style={{ color: 'var(--text-secondary)' }}>Most booked Dhan-Gaadi intercity routes</p>
            </div>
            <div className={styles.routeGrid}>
              {POPULAR_ROUTES.map((route, i) => (
                <button 
                  key={i} 
                  className={styles.routeCard}
                  onClick={() => handlePopularRoute(route.from, route.to)}
                >
                  <div className={styles.routeHeader}>
                    <span className={styles.routeBadge}>{route.routeNo}</span>
                    <span className={styles.routePrice}>{route.price}</span>
                  </div>
                  <div className={styles.routePath}>
                    <div className={styles.routeCity}>
                      <span className={styles.cityDot}>●</span>
                      <strong title={route.from}>{route.from}</strong>
                    </div>
                    <div className={styles.routeLine}>
                      <span className={styles.routeDuration}>{route.duration}</span>
                    </div>
                    <div className={styles.routeCity}>
                      <span className={`${styles.cityDot} ${styles.cityDotEnd}`}>●</span>
                      <strong title={route.to}>{route.to}</strong>
                    </div>
                  </div>
                  <span className={styles.routeCTA}>Book Now →</span>
                </button>
              ))}
            </div>
          </div>
        </section>

        {/* Features */}
        <section className={styles.features}>
          <div className="container">
            <div className={styles.sectionHeader}>
              <h2 className="heading-2">Why Choose Dhan-Gaadi Fleet OS?</h2>
            </div>
            <div className={styles.featureGrid}>
              {[
                { icon: '🛡️', title: 'Safe & Reliable', desc: 'Government-operated fleet with trained drivers and regular maintenance.' },
                { icon: '💰', title: 'Affordable Fares', desc: 'Subsidized government rates — travel across Haryana without breaking the bank.' },
                { icon: '📱', title: 'Easy Online Booking', desc: 'Book from your phone or computer. Select your seats, pay, and get your e-ticket.' },
                { icon: '🎫', title: 'Instant E-Tickets', desc: 'Get your ticket PDF immediately after payment confirmation. Show QR to conductor.' },
                { icon: '🔄', title: 'Live Seat Availability', desc: 'Real-time seat status — see exactly which seats are available before you book.' },
                { icon: '📞', title: '24/7 Support', desc: 'Helpline available round the clock for booking assistance and queries.' },
              ].map((feat, i) => (
                <div key={i} className={styles.featureCard}>
                  <div className={styles.featureIcon}>{feat.icon}</div>
                  <h3 className="heading-4">{feat.title}</h3>
                  <p className="text-small" style={{ color: 'var(--text-secondary)' }}>{feat.desc}</p>
                </div>
              ))}
            </div>
          </div>
        </section>
      </main>
      <Footer />
    </>
  );
}
