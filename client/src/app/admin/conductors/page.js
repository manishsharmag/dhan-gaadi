'use client';
import { useState, useEffect, useCallback } from 'react';
import { authAPI } from '@/lib/api';

export default function ConductorsPage() {
  const [conductors, setConductors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const [showAddModal, setShowAddModal] = useState(false);
  const [newConductor, setNewConductor] = useState({ name: '', mobile: '', password: '' });
  const [submitting, setSubmitting] = useState(false);

  const fetchConductors = useCallback(async () => {
    try {
      setLoading(true);
      const res = await authAPI.getConductors();
      setConductors(res.data || []);
    } catch (err) {
      setError(err.message || 'Failed to load conductors');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchConductors();
  }, [fetchConductors]);

  const handleAddConductor = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await authAPI.createConductor(newConductor);
      setShowAddModal(false);
      setNewConductor({ name: '', mobile: '', password: '' });
      fetchConductors();
    } catch (err) {
      setError(err.message || 'Failed to add conductor');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteConductor = async (id, name) => {
    if (!confirm(`Are you sure you want to remove conductor "${name}"?`)) return;
    try {
      await authAPI.deleteConductor(id);
      fetchConductors();
    } catch (err) {
      alert('Failed to delete conductor: ' + (err.message || 'Unknown error'));
    }
  };

  if (loading && conductors.length === 0) {
    return <div className="loading-overlay"><div className="spinner" /></div>;
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <div>
          <h1 className="heading-2">Manage Conductors</h1>
          <p className="text-body" style={{ color: 'var(--text-secondary)' }}>
            Register conductors to allow POS walk-in bookings and bus tracking on the Android App
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
          + Add Conductor
        </button>
      </div>

      {error && (
        <div style={{ background: 'var(--danger-light)', color: '#991b1b', padding: '10px', borderRadius: '8px', marginBottom: '15px' }}>
          ❌ {error}
        </div>
      )}

      {/* Conductors List */}
      <div style={{ background: 'white', borderRadius: '12px', boxShadow: 'var(--shadow-sm)', overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead style={{ background: 'var(--bg-secondary)' }}>
            <tr>
              <th style={{ padding: '16px', textAlign: 'left', fontWeight: '600' }}>Name</th>
              <th style={{ padding: '16px', textAlign: 'left', fontWeight: '600' }}>Mobile</th>
              <th style={{ padding: '16px', textAlign: 'left', fontWeight: '600' }}>Role</th>
              <th style={{ padding: '16px', textAlign: 'right', fontWeight: '600' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {conductors.length === 0 ? (
              <tr>
                <td colSpan="4" style={{ padding: '24px', textAlign: 'center', color: 'var(--text-secondary)' }}>
                  No conductors registered yet.
                </td>
              </tr>
            ) : (
              conductors.map(c => (
                <tr key={c._id} style={{ borderTop: '1px solid var(--border-color)' }}>
                  <td style={{ padding: '16px', fontWeight: '500' }}>{c.name}</td>
                  <td style={{ padding: '16px' }}>+91 {c.mobile}</td>
                  <td style={{ padding: '16px' }}>
                    <span style={{ background: 'var(--info-light)', color: '#1e40af', padding: '4px 8px', borderRadius: '12px', fontSize: '0.8rem', fontWeight: '600' }}>
                      {c.role?.toUpperCase() || 'CONDUCTOR'}
                    </span>
                  </td>
                  <td style={{ padding: '16px', textAlign: 'right' }}>
                    <button 
                      className="btn btn-ghost btn-sm" 
                      style={{ color: 'var(--danger)' }}
                      onClick={() => handleDeleteConductor(c._id, c.name)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Add Conductor Modal */}
      {showAddModal && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ background: 'white', padding: '24px', borderRadius: '12px', width: '100%', maxWidth: '400px' }}>
            <h2 className="heading-3" style={{ marginBottom: '16px' }}>Add New Conductor</h2>
            <form onSubmit={handleAddConductor}>
              <div className="form-group" style={{ marginBottom: '16px' }}>
                <label className="form-label">Full Name</label>
                <input 
                  type="text" 
                  className="form-input" 
                  value={newConductor.name}
                  onChange={(e) => setNewConductor({...newConductor, name: e.target.value})}
                  required 
                />
              </div>
              <div className="form-group" style={{ marginBottom: '16px' }}>
                <label className="form-label">Mobile Number</label>
                <input 
                  type="tel" 
                  className="form-input"
                  pattern="[6-9][0-9]{9}" 
                  maxLength={10}
                  value={newConductor.mobile}
                  onChange={(e) => setNewConductor({...newConductor, mobile: e.target.value.replace(/\D/g, '')})}
                  required 
                />
              </div>
              <div className="form-group" style={{ marginBottom: '24px' }}>
                <label className="form-label">Password</label>
                <input 
                  type="password" 
                  className="form-input" 
                  minLength={6}
                  value={newConductor.password}
                  onChange={(e) => setNewConductor({...newConductor, password: e.target.value})}
                  required 
                />
              </div>
              
              <div style={{ display: 'flex', gap: '12px' }}>
                <button type="button" className="btn btn-outline" style={{ flex: 1 }} onClick={() => setShowAddModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }} disabled={submitting}>
                  {submitting ? 'Adding...' : 'Add Conductor'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
