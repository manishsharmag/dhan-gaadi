'use client';
import { useState, useEffect, useCallback, Suspense, use } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Navbar from '@/components/Navbar';
import SeatLayout from '@/components/SeatLayout';
import { useAuth } from '@/lib/auth';
import { scheduleAPI, bookingAPI } from '@/lib/api';
import styles from './booking.module.css';

function BookingContent({ explicitScheduleId }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const scheduleId = explicitScheduleId || searchParams.get('scheduleId');
  const { user, isAuthenticated, loading: authLoading } = useAuth();
  
  const date = searchParams.get('date') || new Date().toISOString().split('T')[0];
  
  const [schedule, setSchedule] = useState(null);
  const [unavailableSeats, setUnavailableSeats] = useState([]);
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [booking, setBooking] = useState(false);
  const [error, setError] = useState('');
  
  // Passenger form
  const [passengerName, setPassengerName] = useState('');
  const [passengerMobile, setPassengerMobile] = useState('');
  const [passengerAge, setPassengerAge] = useState('');
  const [passengerGender, setPassengerGender] = useState('');
  const [boardingPoint, setBoardingPoint] = useState('');
  const [droppingPoint, setDroppingPoint] = useState('');
  const [passengers, setPassengers] = useState([]);
  const [step, setStep] = useState('seats'); // 'seats' or 'details'

  const fetchData = useCallback(async () => {
    if (!scheduleId) return;
    setLoading(true);
    try {
      const [scheduleRes, seatsRes] = await Promise.all([
        scheduleAPI.getById(scheduleId),
        bookingAPI.getSeatAvailability(scheduleId, date),
      ]);
      const sched = scheduleRes.data;
      setSchedule(sched);
      setBoardingPoint(sched?.route?.source || '');
      setDroppingPoint(sched?.route?.destination || '');
      // SeatLayout expects array of { seatNumber, status } objects
      setUnavailableSeats(seatsRes.data?.unavailableSeats || []);
    } catch (err) {
      setError(err.message || 'Failed to load booking data');
    } finally {
      setLoading(false);
    }
  }, [scheduleId, date]);

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
    if (scheduleId) {
      fetchData();
    }
  }, [scheduleId, isAuthenticated, authLoading, fetchData, router]);

  const handleSeatClick = (seatNum) => {
    setSelectedSeats(prev => 
      prev.includes(seatNum) 
        ? prev.filter(s => s !== seatNum)
        : prev.length < 6 ? [...prev, seatNum] : prev // Max 6 seats
    );
  };

  const fare = schedule?.fare || schedule?.route?.baseFare || 0;
  const totalFare = fare * selectedSeats.length;

  const handleProceedToDetails = () => {
    if (selectedSeats.length === 0) return;
    const sorted = [...selectedSeats].sort((a, b) => a - b);
    const initialPassengers = sorted.map((seatNum, idx) => ({
      seatNumber: seatNum,
      name: idx === 0 ? (user?.name || passengerName || '') : '',
      age: idx === 0 ? (passengerAge || '') : '',
      gender: idx === 0 ? (passengerGender || '') : '',
    }));
    setPassengers(initialPassengers);
    setPassengerMobile(user?.mobile || passengerMobile || '');
    setPassengerName(user?.name || passengerName || '');
    if (!boardingPoint && schedule?.route?.source) {
      setBoardingPoint(schedule.route.source);
    }
    if (!droppingPoint && schedule?.route?.destination) {
      setDroppingPoint(schedule.route.destination);
    }
    setStep('details');
  };

  const updatePassenger = (index, field, value) => {
    setPassengers(prev => {
      const next = [...prev];
      next[index] = { ...next[index], [field]: value };
      if (index === 0 && field === 'name') {
        setPassengerName(value);
      }
      if (index === 0 && field === 'age') {
        setPassengerAge(value);
      }
      if (index === 0 && field === 'gender') {
        setPassengerGender(value);
      }
      return next;
    });
  };

  const handleConfirmBooking = async (e) => {
    e.preventDefault();
    setBooking(true);
    setError('');

    // Primary passenger name check
    const primaryName = (passengers[0]?.name || passengerName || '').trim();
    if (!primaryName) {
      setError('Primary passenger name is required');
      setBooking(false);
      return;
    }

    try {
      const res = await bookingAPI.create({
        scheduleId,
        journeyDate: date,
        seats: [...selectedSeats].sort((a, b) => a - b),
        passengerName: primaryName,
        passengerMobile,
        passengerAge: passengerAge ? parseInt(passengerAge) : (passengers[0]?.age ? parseInt(passengers[0].age) : undefined),
        passengerGender: passengerGender || passengers[0]?.gender || undefined,
        boardingPoint: boardingPoint || schedule?.route?.source || '',
        droppingPoint: droppingPoint || schedule?.route?.destination || '',
        passengers: passengers.length > 0 ? passengers.map(p => ({
          seatNumber: p.seatNumber,
          name: (p.name || primaryName).trim(),
          age: p.age ? parseInt(p.age) : undefined,
          gender: p.gender || undefined,
        })) : undefined,
      });
      // Navigate to payment page
      router.push(`/payment?bookingId=${res.data.booking.id || res.data.booking._id}`);
    } catch (err) {
      setError(err.message || 'Failed to create booking');
    } finally {
      setBooking(false);
    }
  };

  if (loading) {
    return (
      <><Navbar /><div className="loading-overlay" style={{ minHeight: '80vh' }}><div className="spinner" /></div></>
    );
  }

  if (error && !schedule) {
    return (
      <>
        <Navbar />
        <div className="empty-state" style={{ minHeight: '60vh' }}>
          <span className="empty-icon">⚠️</span>
          <p>{error}</p>
          <button onClick={() => router.back()} className="btn btn-primary">Go Back</button>
        </div>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <main className={styles.page}>
        <div className="container">
          {/* Route info banner */}
          <div className={styles.routeBanner}>
            <div className={styles.routeInfo}>
              <span className={styles.routeNumber}>{schedule?.route?.routeNumber}</span>
              <h1 className="heading-3">{schedule?.route?.source} → {schedule?.route?.destination}</h1>
              <div className={styles.routeMeta}>
                <span>🕐 {schedule?.departureTime}</span>
                <span>🚌 {schedule?.bus?.busNumber} ({schedule?.bus?.type})</span>
                <span>📅 {new Date(date).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}</span>
              </div>
            </div>
            <div className={styles.fareInfo}>
              <span className={styles.fareLabel}>₹{fare}/seat</span>
            </div>
          </div>

          {error && (
            <div className={styles.errorBanner}>❌ {error}</div>
          )}

          {step === 'seats' ? (
            <div className={styles.bookingGrid}>
              {/* Seat layout */}
              <div className={styles.seatSection}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
                  <h2 className="heading-3" style={{ margin: 0 }}>Select Your Seats</h2>
                  <button 
                    type="button" 
                    onClick={fetchData} 
                    className="btn btn-ghost btn-sm"
                    style={{ fontSize: '0.8125rem', gap: '0.35rem' }}
                    title="Refresh live seat status"
                  >
                    🔄 Refresh Chart
                  </button>
                </div>
                <SeatLayout
                  totalSeats={schedule?.bus?.totalSeats || 45}
                  unavailableSeats={unavailableSeats}
                  selectedSeats={selectedSeats}
                  onSeatClick={handleSeatClick}
                />
              </div>

              {/* Booking summary */}
              <div className={styles.summary}>
                <div className={styles.summaryCard}>
                  <h3 className="heading-4">Booking Summary</h3>
                  
                  {selectedSeats.length > 0 ? (
                    <>
                      <div className={styles.summaryRow}>
                        <span>Selected Seats</span>
                        <strong>{[...selectedSeats].sort((a, b) => a - b).join(', ')}</strong>
                      </div>
                      <div className={styles.summaryRow}>
                        <span>Number of Seats</span>
                        <span>{selectedSeats.length}</span>
                      </div>
                      <div className={styles.summaryRow}>
                        <span>Fare per Seat</span>
                        <span>₹{fare}</span>
                      </div>
                      <div className={`${styles.summaryRow} ${styles.summaryTotal}`}>
                        <strong>Total Fare</strong>
                        <strong className={styles.totalAmount}>₹{totalFare}</strong>
                      </div>
                      <button 
                        className="btn btn-primary btn-lg btn-full"
                        onClick={handleProceedToDetails}
                      >
                        Continue → Passenger Details
                      </button>
                    </>
                  ) : (
                    <p className={styles.summaryEmpty}>
                      Click on available seats to select them. You can select up to 6 seats.
                    </p>
                  )}
                </div>
              </div>
            </div>
          ) : (
            /* Passenger Details Form */
            <div className={styles.detailsSection}>
              <button className="btn btn-ghost" onClick={() => setStep('seats')} style={{ marginBottom: '1rem' }}>
                ← Back to Seat Selection
              </button>
              
              <div className={styles.detailsGrid}>
                <form onSubmit={handleConfirmBooking} className={styles.detailsForm}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                    <h2 className="heading-3" style={{ margin: 0 }}>Passenger Details</h2>
                    {user && (
                      <div style={{ display: 'flex', gap: '0.5rem' }}>
                        <button
                          type="button"
                          className="btn btn-outline btn-sm"
                          style={{ fontSize: '0.75rem', padding: '0.25rem 0.65rem' }}
                          onClick={() => {
                            setPassengerName(user.name || '');
                            setPassengerMobile(user.mobile || '');
                          }}
                        >
                          👤 Self (Auto-fill)
                        </button>
                        <button
                          type="button"
                          className="btn btn-ghost btn-sm"
                          style={{ fontSize: '0.75rem', padding: '0.25rem 0.65rem' }}
                          onClick={() => {
                            setPassengerName('');
                            setPassengerMobile('');
                          }}
                        >
                          👥 Someone Else
                        </button>
                      </div>
                    )}
                  </div>
                  
                  {/* Boarding & Dropping Points */}
                  <div className={styles.formRow}>
                    <div className="form-group">
                      <label className="form-label" htmlFor="bpoint">Boarding Station *</label>
                      <select
                        id="bpoint"
                        className="form-select"
                        value={boardingPoint}
                        onChange={(e) => setBoardingPoint(e.target.value)}
                        required
                      >
                        <option value={schedule?.route?.source || ''}>{schedule?.route?.source} (Origin Terminal)</option>
                        {Array.isArray(schedule?.route?.stops) && schedule.route.stops.map((st, i) => (
                          <option key={i} value={st.name}>{st.name} (Stop {st.order || i + 1})</option>
                        ))}
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="dpoint">Dropping Station *</label>
                      <select
                        id="dpoint"
                        className="form-select"
                        value={droppingPoint}
                        onChange={(e) => setDroppingPoint(e.target.value)}
                        required
                      >
                        {Array.isArray(schedule?.route?.stops) && schedule.route.stops.map((st, i) => (
                          <option key={i} value={st.name}>{st.name} (Stop {st.order || i + 1})</option>
                        ))}
                        <option value={schedule?.route?.destination || ''}>{schedule?.route?.destination} (Final Destination)</option>
                      </select>
                    </div>
                  </div>

                  {/* Primary Contact Mobile */}
                  <div className="form-group">
                    <label className="form-label" htmlFor="pmobile">Primary Mobile Number (for SMS & e-Ticket) *</label>
                    <input
                      id="pmobile"
                      type="tel"
                      className="form-input"
                      placeholder="10-digit Indian mobile number"
                      value={passengerMobile}
                      onChange={(e) => setPassengerMobile(e.target.value.replace(/\D/g, '').slice(0, 10))}
                      pattern="[6-9][0-9]{9}"
                      maxLength={10}
                      required
                    />
                  </div>

                  {/* Dynamic Passengers for Each Selected Seat */}
                  {passengers.length > 1 ? (
                    <div style={{ margin: '1rem 0' }}>
                      <label className="form-label" style={{ fontWeight: 700, color: 'var(--text-primary)', marginBottom: '0.75rem', display: 'block' }}>
                        Passenger Details per Seat ({passengers.length} Passengers)
                      </label>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                        {passengers.map((p, idx) => (
                          <div
                            key={p.seatNumber}
                            style={{
                              padding: '1rem',
                              background: idx === 0 ? 'var(--primary-50, #f0fdfa)' : '#f8fafc',
                              border: idx === 0 ? '1.5px solid var(--primary-300, #99f6e4)' : '1px solid #e2e8f0',
                              borderRadius: '12px'
                            }}
                          >
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                              <span style={{ fontWeight: 700, fontSize: '0.9rem', color: 'var(--primary-800, #115e59)' }}>
                                💺 Seat {p.seatNumber} {idx === 0 && <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--primary-600)', marginLeft: '6px' }}>(Primary Passenger)</span>}
                              </span>
                            </div>

                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '0.5rem' }}>
                              <div className="form-group" style={{ margin: 0 }}>
                                <label className="form-label" style={{ fontSize: '0.75rem' }}>Full Name *</label>
                                <input
                                  type="text"
                                  className="form-input"
                                  placeholder={`Passenger ${idx + 1} Name`}
                                  value={p.name}
                                  onChange={(e) => updatePassenger(idx, 'name', e.target.value)}
                                  required
                                  style={{ padding: '0.5rem 0.75rem', fontSize: '0.875rem' }}
                                />
                              </div>
                              <div className="form-group" style={{ margin: 0 }}>
                                <label className="form-label" style={{ fontSize: '0.75rem' }}>Age</label>
                                <input
                                  type="number"
                                  className="form-input"
                                  placeholder="Age"
                                  value={p.age}
                                  onChange={(e) => updatePassenger(idx, 'age', e.target.value)}
                                  min={1}
                                  max={120}
                                  style={{ padding: '0.5rem 0.75rem', fontSize: '0.875rem' }}
                                />
                              </div>
                              <div className="form-group" style={{ margin: 0 }}>
                                <label className="form-label" style={{ fontSize: '0.75rem' }}>Gender</label>
                                <select
                                  className="form-select"
                                  value={p.gender}
                                  onChange={(e) => updatePassenger(idx, 'gender', e.target.value)}
                                  style={{ padding: '0.5rem 0.75rem', fontSize: '0.875rem' }}
                                >
                                  <option value="">Select</option>
                                  <option value="male">Male</option>
                                  <option value="female">Female</option>
                                  <option value="other">Other</option>
                                </select>
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : (
                    /* Single Passenger Layout */
                    <>
                      <div className="form-group">
                        <label className="form-label" htmlFor="pname">Full Name (as per Govt ID) *</label>
                        <input
                          id="pname"
                          type="text"
                          className="form-input"
                          placeholder="Enter passenger name"
                          value={passengerName}
                          onChange={(e) => {
                            setPassengerName(e.target.value);
                            if (passengers[0]) updatePassenger(0, 'name', e.target.value);
                          }}
                          required
                        />
                      </div>
                      
                      <div className={styles.formRow}>
                        <div className="form-group">
                          <label className="form-label" htmlFor="page">Age</label>
                          <input
                            id="page"
                            type="number"
                            className="form-input"
                            placeholder="Age"
                            value={passengerAge}
                            onChange={(e) => {
                              setPassengerAge(e.target.value);
                              if (passengers[0]) updatePassenger(0, 'age', e.target.value);
                            }}
                            min={1}
                            max={120}
                          />
                        </div>
                        <div className="form-group">
                          <label className="form-label" htmlFor="pgender">Gender</label>
                          <select
                            id="pgender"
                            className="form-select"
                            value={passengerGender}
                            onChange={(e) => {
                              setPassengerGender(e.target.value);
                              if (passengers[0]) updatePassenger(0, 'gender', e.target.value);
                            }}
                          >
                            <option value="">Select</option>
                            <option value="male">Male</option>
                            <option value="female">Female</option>
                            <option value="other">Other</option>
                          </select>
                        </div>
                      </div>
                    </>
                  )}
                  
                  <button type="submit" className="btn btn-primary btn-lg btn-full" disabled={booking}>
                    {booking ? 'Locking Seats...' : `Lock Seats & Pay ₹${totalFare}`}
                  </button>
                </form>
                
                {/* Side summary */}
                <div className={styles.summaryCard}>
                  <h3 className="heading-4">Summary</h3>
                  <div className={styles.summaryRow}>
                    <span>Route</span>
                    <strong>{schedule?.route?.source} → {schedule?.route?.destination}</strong>
                  </div>
                  {boardingPoint && (
                    <div className={styles.summaryRow}>
                      <span>Boarding Station</span>
                      <strong>{boardingPoint}</strong>
                    </div>
                  )}
                  {droppingPoint && (
                    <div className={styles.summaryRow}>
                      <span>Dropping Station</span>
                      <strong>{droppingPoint}</strong>
                    </div>
                  )}
                  <div className={styles.summaryRow}>
                    <span>Date</span>
                    <span>{new Date(date).toLocaleDateString('en-IN')}</span>
                  </div>
                  <div className={styles.summaryRow}>
                    <span>Departure</span>
                    <span>{schedule?.departureTime}</span>
                  </div>
                  <div className={styles.summaryRow}>
                    <span>Seats</span>
                    <strong>{[...selectedSeats].sort((a, b) => a - b).join(', ')}</strong>
                  </div>
                  <div className={`${styles.summaryRow} ${styles.summaryTotal}`}>
                    <strong>Total</strong>
                    <strong className={styles.totalAmount}>₹{totalFare}</strong>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </main>
    </>
  );
}

export default function BookingPage({ routeScheduleId }) {
  return (
    <Suspense fallback={<div className="loading-overlay" style={{ minHeight: '100vh' }}><div className="spinner" /></div>}>
      <BookingContent explicitScheduleId={routeScheduleId} />
    </Suspense>
  );
}
