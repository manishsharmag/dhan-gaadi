'use client';
import { useState, useEffect, useCallback } from 'react';
import { routeAPI } from '@/lib/api';

export default function AdminRoutes() {
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState({
    routeNumber: '', name: '', source: '', destination: '',
    sourceDepot: '', destinationDepot: '', totalDistance: '',
    baseFare: '', stops: ''
  });

  const fetchRoutes = useCallback(async () => {
    try {
      const res = await routeAPI.getAll();
      setRoutes(res.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchRoutes(); }, [fetchRoutes]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const data = {
      ...form,
      totalDistance: parseInt(form.totalDistance),
      baseFare: parseInt(form.baseFare),
      stops: form.stops ? form.stops.split(',').map((s, i) => ({
        name: s.trim(), distanceFromSource: 0, order: i + 1
      })) : [],
    };
    try {
      if (editId) {
        await routeAPI.update(editId, data);
      } else {
        await routeAPI.create(data);
      }
      resetForm();
      fetchRoutes();
    } catch (err) {
      alert('Error: ' + (err.message || 'Failed'));
    }
  };

  const handleEdit = (route) => {
    setEditId(route._id);
    setForm({
      routeNumber: route.routeNumber,
      name: route.name,
      source: route.source,
      destination: route.destination,
      sourceDepot: route.sourceDepot || '',
      destinationDepot: route.destinationDepot || '',
      totalDistance: route.totalDistance?.toString() || '',
      baseFare: route.baseFare?.toString() || '',
      stops: route.stops?.map(s => s.name).join(', ') || '',
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this route?')) return;
    try {
      await routeAPI.delete(id);
      fetchRoutes();
    } catch (err) {
      alert('Delete failed');
    }
  };

  const resetForm = () => {
    setShowForm(false);
    setEditId(null);
    setForm({ routeNumber: '', name: '', source: '', destination: '', sourceDepot: '', destinationDepot: '', totalDistance: '', baseFare: '', stops: '' });
  };

  if (loading) return <div className="loading-overlay"><div className="spinner" /></div>;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h1 className="heading-2">Route Management</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? '✕ Cancel' : '+ Add Route'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="card" style={{ marginBottom: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <h3 className="heading-4">{editId ? 'Edit Route' : 'Add New Route'}</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '1rem' }}>
            <div className="form-group">
              <label className="form-label">Route Number (HR-XX) *</label>
              <input className="form-input" placeholder="HR-38" value={form.routeNumber} onChange={e => setForm({...form, routeNumber: e.target.value})} required />
            </div>
            <div className="form-group">
              <label className="form-label">Name *</label>
              <input className="form-input" placeholder="Hisar - Chandigarh" value={form.name} onChange={e => setForm({...form, name: e.target.value})} required />
            </div>
            <div className="form-group">
              <label className="form-label">Source *</label>
              <input className="form-input" placeholder="Hisar" value={form.source} onChange={e => setForm({...form, source: e.target.value})} required />
            </div>
            <div className="form-group">
              <label className="form-label">Destination *</label>
              <input className="form-input" placeholder="Chandigarh" value={form.destination} onChange={e => setForm({...form, destination: e.target.value})} required />
            </div>
            <div className="form-group">
              <label className="form-label">Source Depot *</label>
              <input className="form-input" placeholder="Hisar Bus Stand" value={form.sourceDepot} onChange={e => setForm({...form, sourceDepot: e.target.value})} required />
            </div>
            <div className="form-group">
              <label className="form-label">Destination Depot</label>
              <input className="form-input" placeholder="ISBT Chandigarh" value={form.destinationDepot} onChange={e => setForm({...form, destinationDepot: e.target.value})} />
            </div>
            <div className="form-group">
              <label className="form-label">Distance (km) *</label>
              <input type="number" className="form-input" value={form.totalDistance} onChange={e => setForm({...form, totalDistance: e.target.value})} required />
            </div>
            <div className="form-group">
              <label className="form-label">Base Fare (₹) *</label>
              <input type="number" className="form-input" value={form.baseFare} onChange={e => setForm({...form, baseFare: e.target.value})} required />
            </div>
            <div className="form-group">
              <label className="form-label">Stops (comma-separated)</label>
              <input className="form-input" placeholder="Hansi, Jind, Karnal" value={form.stops} onChange={e => setForm({...form, stops: e.target.value})} />
            </div>
          </div>
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button type="submit" className="btn btn-primary">{editId ? 'Update Route' : 'Add Route'}</button>
            <button type="button" className="btn btn-ghost" onClick={resetForm}>Cancel</button>
          </div>
        </form>
      )}

      <div className="table-container">
        <table className="table">
          <thead>
            <tr>
              <th>Route No.</th>
              <th>Name</th>
              <th>Source → Dest</th>
              <th>Distance</th>
              <th>Fare</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {routes.map(r => (
              <tr key={r._id}>
                <td><strong>{r.routeNumber}</strong></td>
                <td>{r.name}</td>
                <td>{r.source} → {r.destination}</td>
                <td>{r.totalDistance} km</td>
                <td>₹{r.baseFare}</td>
                <td><span className={`badge ${r.status === 'active' ? 'badge-success' : 'badge-neutral'}`}>{r.status}</span></td>
                <td>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <button className="btn btn-ghost btn-sm" onClick={() => handleEdit(r)}>✏️</button>
                    <button className="btn btn-ghost btn-sm" onClick={() => handleDelete(r._id)}>🗑️</button>
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
