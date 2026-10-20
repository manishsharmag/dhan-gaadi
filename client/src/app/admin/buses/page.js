'use client';
import { useState, useEffect, useCallback } from 'react';
import { busAPI } from '@/lib/api';

export default function AdminBuses() {
  const [buses, setBuses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState({
    busNumber: '', name: '', type: 'ordinary', totalSeats: '45', depot: '', amenities: ''
  });

  const fetchBuses = useCallback(async () => {
    try {
      const res = await busAPI.getAll();
      setBuses(res.data || []);
    } catch (err) { console.error(err); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchBuses(); }, [fetchBuses]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const data = {
      ...form,
      totalSeats: parseInt(form.totalSeats),
      amenities: form.amenities ? form.amenities.split(',').map(a => a.trim()) : [],
    };
    try {
      if (editId) await busAPI.update(editId, data);
      else await busAPI.create(data);
      resetForm(); fetchBuses();
    } catch (err) { alert('Error: ' + (err.message || 'Failed')); }
  };

  const handleEdit = (bus) => {
    setEditId(bus._id);
    setForm({
      busNumber: bus.busNumber, name: bus.name || '', type: bus.type,
      totalSeats: bus.totalSeats?.toString(), depot: bus.depot,
      amenities: bus.amenities?.join(', ') || ''
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this bus?')) return;
    try { await busAPI.delete(id); fetchBuses(); }
    catch (err) { alert('Delete failed'); }
  };

  const resetForm = () => {
    setShowForm(false); setEditId(null);
    setForm({ busNumber: '', name: '', type: 'ordinary', totalSeats: '45', depot: '', amenities: '' });
  };

  if (loading) return <div className="loading-overlay"><div className="spinner" /></div>;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h1 className="heading-2">Bus Management</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? '✕ Cancel' : '+ Add Bus'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="card" style={{ marginBottom: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <h3 className="heading-4">{editId ? 'Edit Bus' : 'Add New Bus'}</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '1rem' }}>
            <div className="form-group">
              <label className="form-label">Bus Number *</label>
              <input className="form-input" placeholder="HR-38-A-1234" value={form.busNumber} onChange={e => setForm({...form, busNumber: e.target.value})} required />
            </div>
            <div className="form-group">
              <label className="form-label">Name</label>
              <input className="form-input" placeholder="Hisar Express" value={form.name} onChange={e => setForm({...form, name: e.target.value})} />
            </div>
            <div className="form-group">
              <label className="form-label">Type *</label>
              <select className="form-select" value={form.type} onChange={e => setForm({...form, type: e.target.value})}>
                <option value="ordinary">Ordinary</option>
                <option value="deluxe">Deluxe</option>
                <option value="volvo-ac">Volvo AC</option>
                <option value="super-deluxe">Super Deluxe</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Total Seats *</label>
              <input type="number" className="form-input" value={form.totalSeats} onChange={e => setForm({...form, totalSeats: e.target.value})} required />
            </div>
            <div className="form-group">
              <label className="form-label">Depot *</label>
              <input className="form-input" placeholder="Hisar" value={form.depot} onChange={e => setForm({...form, depot: e.target.value})} required />
            </div>
            <div className="form-group">
              <label className="form-label">Amenities (comma-separated)</label>
              <input className="form-input" placeholder="AC, WiFi, Charging" value={form.amenities} onChange={e => setForm({...form, amenities: e.target.value})} />
            </div>
          </div>
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button type="submit" className="btn btn-primary">{editId ? 'Update' : 'Add Bus'}</button>
            <button type="button" className="btn btn-ghost" onClick={resetForm}>Cancel</button>
          </div>
        </form>
      )}

      <div className="table-container">
        <table className="table">
          <thead>
            <tr><th>Bus No.</th><th>Name</th><th>Type</th><th>Seats</th><th>Depot</th><th>Status</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {buses.map(b => (
              <tr key={b._id}>
                <td><strong>{b.busNumber}</strong></td>
                <td>{b.name}</td>
                <td><span className="badge badge-info">{b.type}</span></td>
                <td>{b.totalSeats}</td>
                <td>{b.depot}</td>
                <td><span className={`badge ${b.status === 'active' ? 'badge-success' : 'badge-neutral'}`}>{b.status}</span></td>
                <td>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <button className="btn btn-ghost btn-sm" onClick={() => handleEdit(b)}>✏️</button>
                    <button className="btn btn-ghost btn-sm" onClick={() => handleDelete(b._id)}>🗑️</button>
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
