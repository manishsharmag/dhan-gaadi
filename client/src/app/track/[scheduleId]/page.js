'use client';
import { useState, useEffect, useRef, useCallback } from 'react';
import { useRouter, useParams } from 'next/navigation';
import Navbar from '@/components/Navbar';
import { BACKEND_ORIGIN } from '@/lib/config';
import { trackingAPI } from '@/lib/api';
import styles from './track.module.css';
import io from 'socket.io-client';

export default function TrackBusPage() {
  const router = useRouter();
  const params = useParams();
  const scheduleId = params.scheduleId;

  const [busLocation, setBusLocation] = useState(null);
  const [speed, setSpeed] = useState(0);
  const [heading, setHeading] = useState(0);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [schedule, setSchedule] = useState(null);
  const [trackingActive, setTrackingActive] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const socketRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const stopsLayerRef = useRef(null);

  // Fetch initial tracking data via REST
  useEffect(() => {
    async function fetchTracking() {
      try {
        const res = await trackingAPI.getLiveBus(scheduleId);
        const data = res.data;
        if (data) {
          setBusLocation(data.location);
          setSpeed(data.speed);
          setHeading(data.heading);
          setLastUpdated(data.lastUpdated);
          setSchedule(data.schedule);
          setTrackingActive(data.isActive);
        } else {
          setError('This bus is not being tracked right now.');
        }
      } catch (err) {
        setError(err.message || 'Failed to connect to tracking server.');
      } finally {
        setLoading(false);
      }
    }
    if (scheduleId) fetchTracking();
  }, [scheduleId]);

  // Connect to Socket.IO for live updates
  useEffect(() => {
    if (!scheduleId) return;

    const socket = io(BACKEND_ORIGIN, {
      transports: ['websocket', 'polling'],
    });
    socketRef.current = socket;

    socket.on('connect', () => {
      socket.emit('bus:subscribe', { scheduleId });
    });

    socket.on('bus:location', (data) => {
      setBusLocation(data.location);
      setSpeed(data.speed);
      setHeading(data.heading);
      setLastUpdated(data.lastUpdated);
      setTrackingActive(true);
    });

    socket.on('bus:tracking-ended', () => {
      setTrackingActive(false);
    });

    return () => {
      socket.emit('bus:unsubscribe', { scheduleId });
      socket.disconnect();
    };
  }, [scheduleId]);

  const busLocationRef = useRef(busLocation);
  useEffect(() => {
    busLocationRef.current = busLocation;
  }, [busLocation]);

  // Initialize Leaflet Map once on mount / script ready
  useEffect(() => {
    if (typeof window === 'undefined') return;

    const loadLeaflet = () => {
      if (!document.getElementById('leaflet-css')) {
        const link = document.createElement('link');
        link.id = 'leaflet-css';
        link.rel = 'stylesheet';
        link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
        document.head.appendChild(link);
      }

      if (!window.L) {
        if (!document.getElementById('leaflet-js')) {
          const script = document.createElement('script');
          script.id = 'leaflet-js';
          script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
          script.onload = () => setupMap();
          document.head.appendChild(script);
        }
      } else {
        setupMap();
      }
    };

    const setupMap = () => {
      const L = window.L;
      if (!L || mapRef.current) return;
      const mapContainer = document.getElementById('tracking-map');
      if (!mapContainer) return;

      const initialLat = busLocationRef.current?.lat || 29.1492;
      const initialLng = busLocationRef.current?.lng || 75.7217;

      const map = L.map('tracking-map').setView([initialLat, initialLng], 14);
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
      }).addTo(map);

      const busIcon = L.divIcon({
        html: '<div style="font-size:32px;filter:drop-shadow(0 2px 4px rgba(0,0,0,0.3));">🚌</div>',
        iconSize: [40, 40],
        iconAnchor: [20, 20],
        className: 'bus-marker',
      });

      markerRef.current = L.marker([initialLat, initialLng], { icon: busIcon }).addTo(map);
      stopsLayerRef.current = L.layerGroup().addTo(map);
      mapRef.current = map;
    };

    loadLeaflet();

    return () => {
      if (mapRef.current) {
        mapRef.current.remove();
        mapRef.current = null;
        markerRef.current = null;
        stopsLayerRef.current = null;
      }
    };
  }, []);

  // Dynamically update stops layer whenever schedule data arrives or updates
  useEffect(() => {
    const L = typeof window !== 'undefined' ? window.L : null;
    if (!L || !mapRef.current || !stopsLayerRef.current) return;

    stopsLayerRef.current.clearLayers();

    if (schedule?.route?.stops && Array.isArray(schedule.route.stops)) {
      const stops = [...schedule.route.stops].sort((a, b) => a.order - b.order);
      const points = [];

      stops.forEach((stop) => {
        if (stop.lat && stop.lng) {
          const latLng = [Number(stop.lat), Number(stop.lng)];
          points.push(latLng);
          L.circleMarker(latLng, {
            radius: 8,
            fillColor: '#0d9488',
            color: '#fff',
            weight: 2,
            fillOpacity: 0.9,
          })
            .bindPopup(`<b>${stop.name}</b><br>${stop.distanceFromSource || 0} km from start`)
            .addTo(stopsLayerRef.current);
        }
      });

      if (points.length > 1) {
        L.polyline(points, { color: '#0d9488', weight: 3, opacity: 0.7, dashArray: '6, 6' }).addTo(stopsLayerRef.current);
      }
    }
  }, [schedule]);

  // Smoothly update marker position and pan map when busLocation updates
  useEffect(() => {
    if (!busLocation || !mapRef.current || !markerRef.current) return;
    const { lat, lng } = busLocation;
    markerRef.current.setLatLng([lat, lng]);
    mapRef.current.panTo([lat, lng], { animate: true, duration: 1 });
  }, [busLocation]);

  const formatTime = (dateStr) => {
    if (!dateStr) return '--';
    const d = new Date(dateStr);
    return d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

  const route = schedule?.route;
  const bus = schedule?.bus;

  if (loading) {
    return (
      <>
        <Navbar />
        <div className="loading-overlay"><div className="spinner" /></div>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <main className={styles.page}>
        <div className="container">
          <div className={styles.header}>
            <div>
              <h1 className="heading-2">
                🚌 Live Bus Tracking
              </h1>
              {route && (
                <p className={styles.routeInfo}>
                  {route.source} → {route.destination} • {bus?.busNumber || ''}
                </p>
              )}
            </div>
            <div className={`${styles.statusBadge} ${trackingActive ? styles.active : styles.inactive}`}>
              <span className={styles.dot} />
              {trackingActive ? 'LIVE' : 'OFFLINE'}
            </div>
          </div>

          {error && !trackingActive ? (
            <div className={styles.errorCard}>
              <span className={styles.errorIcon}>📡</span>
              <h3>Bus Not Being Tracked</h3>
              <p>{error}</p>
              <p className={styles.hint}>Tracking starts when the conductor begins their trip.</p>
              <button onClick={() => router.back()} className="btn btn-primary">Go Back</button>
            </div>
          ) : (
            <>
              {/* Map */}
              <div className={styles.mapContainer}>
                <div id="tracking-map" className={styles.map} />
              </div>

              {/* Info Cards */}
              <div className={styles.infoGrid}>
                <div className={styles.infoCard}>
                  <span className={styles.infoLabel}>Speed</span>
                  <span className={styles.infoValue}>{speed} km/h</span>
                </div>
                <div className={styles.infoCard}>
                  <span className={styles.infoLabel}>Last Update</span>
                  <span className={styles.infoValue}>{formatTime(lastUpdated)}</span>
                </div>
                <div className={styles.infoCard}>
                  <span className={styles.infoLabel}>Heading</span>
                  <span className={styles.infoValue}>{Math.round(heading)}°</span>
                </div>
                <div className={styles.infoCard}>
                  <span className={styles.infoLabel}>Status</span>
                  <span className={styles.infoValue}>{trackingActive ? '✅ Active' : '⛔ Stopped'}</span>
                </div>
              </div>

              {/* Assigned Conductor Card */}
              {schedule?.conductor && (
                <div style={{
                  marginTop: '1.25rem',
                  padding: '1rem 1.25rem',
                  background: '#f0fdfa',
                  border: '1px solid #ccfbf1',
                  borderRadius: '16px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: '1rem',
                  flexWrap: 'wrap'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
                    <div style={{
                      width: '48px', height: '48px', borderRadius: '50%',
                      background: '#ccfbf1', display: 'flex',
                      alignItems: 'center', justifyContent: 'center', fontSize: '1.5rem',
                      overflow: 'hidden', flexShrink: 0
                    }}>
                      {schedule.conductor.photo ? (
                        <img src={schedule.conductor.photo} alt={schedule.conductor.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                      ) : '👮'}
                    </div>
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '1.05rem', color: '#134e4a' }}>
                        {schedule.conductor.name}
                      </div>
                      <div style={{ fontSize: '0.8rem', color: '#0f766e' }}>
                        Assigned Bus Conductor • Dhan-Gaadi Fleet
                      </div>
                      <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#0f766e', marginTop: '2px' }}>
                        📞 +91 {schedule.conductor.mobile}
                      </div>
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <a href={`tel:${schedule.conductor.mobile}`} className="btn btn-outline btn-sm">
                      📞 Call Conductor
                    </a>
                    <a
                      href={`https://api.whatsapp.com/send?phone=91${schedule.conductor.mobile.replace(/\D/g, '').slice(-10)}&text=${encodeURIComponent(`Hello Conductor ${schedule.conductor.name}, I am tracking your bus (${schedule.bus?.busNumber || ''}).`)}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="btn btn-sm"
                      style={{ background: '#25D366', color: '#fff' }}
                    >
                      💬 WhatsApp
                    </a>
                  </div>
                </div>
              )}

              {/* Stops Timeline */}
              {route?.stops?.length > 0 && (
                <div className={styles.stopsCard}>
                  <h3 className="heading-4">Route Stops</h3>
                  <div className={styles.timeline}>
                    {[...route.stops]
                      .sort((a, b) => a.order - b.order)
                      .map((stop, i, arr) => (
                        <div key={i} className={styles.timelineItem}>
                          <div className={styles.timelineDot} />
                          {i < arr.length - 1 && <div className={styles.timelineLine} />}
                          <div className={styles.timelineContent}>
                            <strong>{stop.name}</strong>
                            <span>{stop.distanceFromSource} km from start</span>
                          </div>
                        </div>
                      ))}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </main>
    </>
  );
}
