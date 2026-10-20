'use client';
import { useState, useEffect, useCallback } from 'react';
import { scheduleAPI, routeAPI, busAPI, authAPI } from '@/lib/api';

const ALL_DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

export default function AdminSchedules() {
  const [schedules, setSchedules] = useState([]);
  const [routes, setRoutes] = useState([]);
  const [buses, setBuses] = useState([]);
  const [conductors, setConductors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState({
    route: '', bus: '', conductor: '', departureTime: '', arrivalTime: '',
    fare: '', daysOfOperation: [...ALL_DAYS]
  });

  const fetchSchedules = useCallback(async () => {
    try {
      const res = await scheduleAPI.getAll();
      setSchedules(res.data || []);
    } catch (err) { console.error(err); }
    finally { setLoading(false); }
  }, []);

  const fetchRoutes = useCallback(async () => {
    try { const res = await routeAPI.getAll(); setRoutes(res.data || []); }
    catch (err) { console.error(err); }
  }, []);

  const fetchBuses = useCallback(async () => {
    try { const res = await busAPI.getAll(); setBuses(res.data || []); }
    catch (err) { console.error(err); }
  }, []);

  const fetchConductors = useCallback(async () => {
    try { const res = await authAPI.getConductors(); setConductors(res.data || []); }
    catch (err) { console.error(err); }
  }, []);

  useEffect(() => {
    Promise.all([fetchSchedules(), fetchRoutes(), fetchBuses(), fetchConductors()]);
  }, [fetchSchedules, fetchRoutes, fetchBuses, fetchConductors]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const data = {
      ...form,
      conductor: form.conductor || null,
      fare: form.fare ? parseInt(form.fare) : undefined,
    };
    try {
      if (editId) await scheduleAPI.update(editId, data);
      else await scheduleAPI.create(data);
      resetForm(); fetchSchedules();
    } catch (err) { alert('Error: ' + (err.message || 'Failed')); }
  };

  const handleDayToggle = (day) => {
    setForm(prev => ({
      ...prev,
      daysOfOperation: prev.daysOfOperation.includes(day)
        ? prev.daysOfOperation.filter(d => d !== day)
        : [...prev.daysOfOperation, day]
    }));
  };

  const handleEdit = (s) => {
    setEditId(s._id);
    setForm({
      route: s.route?._id || '', bus: s.bus?._id || '',
      conductor: s.conductor?._id || '',
      departureTime: s.departureTime, arrivalTime: s.arrivalTime || '',
      fare: s.fare?.toString() || '',
      daysOfOperation: s.daysOfOperation || [...ALL_DAYS]
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this schedule?')) return;
    try { await scheduleAPI.delete(id); fetchSchedules(); }
    catch (err) { alert('Delete failed'); }
  };

  const resetForm = () => {
    setShowForm(false); setEditId(null);
    setForm({ route: '', bus: '', conductor: '', departureTime: '', arrivalTime: '', fare: '', daysOfOperation: [...ALL_DAYS] });
  };

  if (loading) return <div className="loading-overlay"><div className="spinner" /></div>;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h1 className="heading-2">Schedule Management</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? '✕ Cancel' : '+ Add Schedule'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="card" style={{ marginBottom: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <h3 className="heading-4">{editId ? 'Edit Schedule' : 'Add New Schedule'}</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div className="form-group">
              <label className="form-label">Route *</label>
              <select className="form-select" value={form.route} onChange={e => setForm({...form, route: e.target.value})} required>
                <option value="">Select Route</option>
                {routes.map(r => <option key={r._id} value={r._id}>{r.routeNumber} — {r.name}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Bus *</label>
              <select className="form-select" value={form.bus} onChange={e => setForm({...form, bus: e.target.value})} required>
                <option value="">Select Bus</option>
                {buses.map(b => <option key={b._id} value={b._id}>{b.busNumber} — {b.name} ({b.type})</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Assigned Conductor</label>
              <select className="form-select" value={form.conductor} onChange={e => setForm({...form, conductor: e.target.value})}>
                <option value="">None (Unassigned)</option>
                {conductors.map(c => <option key={c._id} value={c._id}>👮 {c.name} ({c.mobile})</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Departure Time *</label>
              <input type="time" className="form-input" value={form.departureTime} onChange={e => setForm({...form, departureTime: e.target.value})} required />
            </div>
            <div className="form-group">
              <label className="form-label">Arrival Time</label>
              <input type="time" className="form-input" value={form.arrivalTime} onChange={e => setForm({...form, arrivalTime: e.target.value})} />
            </div>
            <div className="form-group">
              <label className="form-label">Fare Override (₹)</label>
              <input type="number" className="form-input" placeholder="Leave empty to use route fare" value={form.fare} onChange={e => setForm({...form, fare: e.target.value})} />
            </div>
            <div className="form-group" style={{ gridColumn: 'span 2' }}>
              <label className="form-label">Days of Operation</label>
              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                {ALL_DAYS.map(day => (
                  <button
                    key={day}
                    type="button"
                    className={`btn btn-sm ${form.daysOfOperation.includes(day) ? 'btn-primary' : 'btn-outline'}`}
                    onClick={() => handleDayToggle(day)}
                    style={{ minWidth: '48px' }}
                  >
                    {day}
                  </button>
                ))}
              </div>
            </div>
          </div>
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button type="submit" className="btn btn-primary">{editId ? 'Update' : 'Add Schedule'}</button>
            <button type="button" className="btn btn-ghost" onClick={resetForm}>Cancel</button>
          </div>
        </form>
      )}

      <div className="table-container">
        <table className="table">
          <thead>
            <tr><th>Route</th><th>Bus</th><th>Conductor</th><th>Departure</th><th>Arrival</th><th>Fare</th><th>Days</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {schedules.map(s => (
              <tr key={s._id}>
                <td><strong>{s.route?.routeNumber}</strong> — {s.route?.name}</td>
                <td>{s.bus?.busNumber} <span className="badge badge-info" style={{ marginLeft: '0.5rem' }}>{s.bus?.type}</span></td>
                <td>
                  {s.conductor ? (
                    <span className="badge badge-info" style={{ fontWeight: 600 }}>
                      👮 {s.conductor.name}
                    </span>
                  ) : (
                    <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Unassigned</span>
                  )}
                </td>
                <td><strong>{s.departureTime}</strong></td>
                <td>{s.arrivalTime || '—'}</td>
                <td>₹{s.fare || s.route?.baseFare}</td>
                <td style={{ fontSize: '0.75rem' }}>{s.daysOfOperation?.join(', ')}</td>
                <td>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <button className="btn btn-ghost btn-sm" onClick={() => handleEdit(s)}>✏️</button>
                    <button className="btn btn-ghost btn-sm" onClick={() => handleDelete(s._id)}>🗑️</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
