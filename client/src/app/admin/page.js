'use client';
import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { bookingAPI } from '@/lib/api';
import styles from './dashboard.module.css';

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchStats = useCallback(async () => {
    try {
      const res = await bookingAPI.getStats();
      setStats(res.data);
    } catch (err) {
      console.error('Failed to fetch stats:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStats();
    // Poll every 30 seconds for updated stats
    const interval = setInterval(fetchStats, 30000);
    return () => clearInterval(interval);
  }, [fetchStats]);

  if (loading) {
    return <div className="loading-overlay"><div className="spinner" /></div>;
  }

  return (
    <div>
      <div className={styles.header}>
        <h1 className="heading-2">Admin Dashboard</h1>
        <p className="text-body" style={{ color: 'var(--text-secondary)' }}>
          Dhan-Gaadi Fleet OS Enterprise Control Panel
        </p>
      </div>

      {/* Stats Grid */}
      <div className={styles.statsGrid}>
        <div className={`stat-card ${styles.statPending}`}>
          <div className="stat-icon" style={{ background: 'var(--warning-light)', color: '#92400e' }}>⏳</div>
          <div className="stat-value">{stats?.pendingVerification || 0}</div>
          <div className="stat-label">Pending Verification</div>
          {stats?.pendingVerification > 0 && (
            <Link href="/admin/bookings" className={styles.statAction}>
              Review Now →
            </Link>
          )}
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--success-light)', color: '#065f46' }}>✅</div>
          <div className="stat-value">{stats?.confirmedBookings || 0}</div>
          <div className="stat-label">Confirmed Bookings</div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--info-light)', color: '#1e40af' }}>🎫</div>
          <div className="stat-value">{stats?.todayBookings || 0}</div>
          <div className="stat-label">Today&apos;s Bookings</div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--primary-50)', color: 'var(--primary-700)' }}>💰</div>
          <div className="stat-value">₹{(stats?.totalRevenue || 0).toLocaleString('en-IN')}</div>
          <div className="stat-label">Total Revenue</div>
        </div>
      </div>

      {/* Quick Actions */}
      <h2 className="heading-3" style={{ marginTop: '2rem', marginBottom: '1rem' }}>Quick Actions</h2>
      <div className={styles.actionsGrid}>
        <Link href="/admin/bookings" className={styles.actionCard}>
          <span className={styles.actionIcon}>✅</span>
          <div>
            <strong>Verify Payments</strong>
            <span>Review and confirm pending bookings</span>
          </div>
          {stats?.pendingVerification > 0 && (
            <span className={styles.actionBadge}>{stats.pendingVerification}</span>
          )}
        </Link>

        <Link href="/admin/qr" className={styles.actionCard}>
          <span className={styles.actionIcon}>📱</span>
          <div>
            <strong>Manage QR Code</strong>
            <span>Upload or change UPI QR code</span>
          </div>
        </Link>

        <Link href="/admin/routes" className={styles.actionCard}>
          <span className={styles.actionIcon}>🛣️</span>
          <div>
            <strong>Manage Routes</strong>
            <span>Add, edit, or remove bus routes</span>
          </div>
        </Link>

        <Link href="/admin/schedules" className={styles.actionCard}>
          <span className={styles.actionIcon}>📅</span>
          <div>
            <strong>Manage Schedules</strong>
            <span>Assign buses to routes with times</span>
          </div>
        </Link>

        <Link href="/admin/reports" className={styles.actionCard}>
          <span className={styles.actionIcon}>📈</span>
          <div>
            <strong>Revenue Reports</strong>
            <span>View reports and export CSV</span>
          </div>
        </Link>

        <Link href="/admin/buses" className={styles.actionCard}>
          <span className={styles.actionIcon}>🚌</span>
          <div>
            <strong>Manage Buses</strong>
            <span>Add, edit, or remove buses</span>
          </div>
        </Link>
        
        <Link href="/admin/conductors" className={styles.actionCard}>
          <span className={styles.actionIcon}>👨‍✈️</span>
          <div>
            <strong>Manage Conductors</strong>
            <span>Register and assign conductors</span>
          </div>
        </Link>
      </div>
    </div>
  );
}
