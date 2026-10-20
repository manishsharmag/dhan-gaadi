'use client';
import { useState, useEffect, useCallback } from 'react';
import { bookingAPI } from '@/lib/api';

export default function AdminReports() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [dateRange, setDateRange] = useState({ start: '', end: '' });

  const fetchStats = useCallback(async () => {
    try {
      const res = await bookingAPI.getStats();
      setStats(res.data);
    } catch (err) { console.error(err); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchStats(); }, [fetchStats]);

  const handleExport = async (status) => {
    try {
      const params = { status };
      if (dateRange.start) params.startDate = dateRange.start;
      if (dateRange.end) params.endDate = dateRange.end;
      const res = await bookingAPI.exportCSV(params);
      const csvData = res.data || res;
      const blob = new Blob([csvData], { type: 'text/csv;charset=utf-8;' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `report-${status || 'all'}-${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert('Export failed: ' + (err.message || 'Unknown error'));
    }
  };

  if (loading) return <div className="loading-overlay"><div className="spinner" /></div>;

  return (
    <div>
      <h1 className="heading-2" style={{ marginBottom: '2rem' }}>Revenue Reports</h1>

      {/* Revenue Stats */}
      <div className="grid-4" style={{ marginBottom: '2rem' }}>
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--primary-50)', color: 'var(--primary-700)' }}>💰</div>
          <div className="stat-value">₹{(stats?.totalRevenue || 0).toLocaleString('en-IN')}</div>
          <div className="stat-label">Total Revenue (Confirmed)</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--success-light)', color: '#065f46' }}>✅</div>
          <div className="stat-value">{stats?.confirmedBookings || 0}</div>
          <div className="stat-label">Confirmed Bookings</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--warning-light)', color: '#92400e' }}>⏳</div>
          <div className="stat-value">{stats?.pendingVerification || 0}</div>
          <div className="stat-label">Pending Verification</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--info-light)', color: '#1e40af' }}>📊</div>
          <div className="stat-value">{stats?.totalBookings || 0}</div>
          <div className="stat-label">Total Bookings</div>
        </div>
      </div>

      {/* Export Section */}
      <div className="card">
        <h3 className="heading-4" style={{ marginBottom: '1rem' }}>Export Data</h3>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'end', flexWrap: 'wrap' }}>
          <div className="form-group">
            <label className="form-label">Start Date</label>
            <input type="date" className="form-input" value={dateRange.start} onChange={e => setDateRange({...dateRange, start: e.target.value})} />
          </div>
          <div className="form-group">
            <label className="form-label">End Date</label>
            <input type="date" className="form-input" value={dateRange.end} onChange={e => setDateRange({...dateRange, end: e.target.value})} />
          </div>
          <button className="btn btn-primary" onClick={() => handleExport('CONFIRMED')}>📥 Export Confirmed</button>
          <button className="btn btn-outline" onClick={() => handleExport('')}>📥 Export All</button>
        </div>
      </div>
    </div>
  );
}
