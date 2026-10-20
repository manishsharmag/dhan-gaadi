'use client';
import { useState, useEffect, useCallback, useRef } from 'react';
import { qrAPI } from '@/lib/api';
import { buildAssetUrl } from '@/lib/config';
import styles from './qr.module.css';

export default function QRManagement() {
  const [qrCodes, setQrCodes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [upiLabel, setUpiLabel] = useState('');
  const [upiId, setUpiId] = useState('');
  const fileInputRef = useRef(null);

  const fetchQRCodes = useCallback(async () => {
    try {
      const res = await qrAPI.getAll();
      setQrCodes(res.data || []);
    } catch (err) {
      console.error('Failed to fetch QR codes:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchQRCodes(); }, [fetchQRCodes]);

  const handleUpload = async (e) => {
    e.preventDefault();
    const file = fileInputRef.current?.files?.[0];
    if (!file || !upiLabel) return alert('Please select an image and enter UPI label');
    
    setUploading(true);
    try {
      const formData = new FormData();
      formData.append('qrImage', file);
      formData.append('upiLabel', upiLabel);
      formData.append('upiId', upiId);
      await qrAPI.upload(formData);
      setUpiLabel('');
      setUpiId('');
      fileInputRef.current.value = '';
      fetchQRCodes();
    } catch (err) {
      alert('Upload failed: ' + (err.message || 'Unknown error'));
    } finally {
      setUploading(false);
    }
  };

  const handleToggle = async (id) => {
    try {
      await qrAPI.activate(id);
      fetchQRCodes();
    } catch (err) {
      alert('Toggle failed: ' + (err.message || 'Unknown error'));
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this QR code?')) return;
    try {
      await qrAPI.delete(id);
      fetchQRCodes();
    } catch (err) {
      alert('Delete failed: ' + (err.message || 'Unknown error'));
    }
  };

  if (loading) return <div className="loading-overlay"><div className="spinner" /></div>;

  return (
    <div>
      <h1 className="heading-2" style={{ marginBottom: '2rem' }}>UPI QR Code Management</h1>
      
      {/* Upload Form */}
      <div className={styles.uploadCard}>
        <h3 className="heading-4">Upload New QR Code</h3>
        <form onSubmit={handleUpload} className={styles.uploadForm}>
          <div className="form-group">
            <label className="form-label">QR Code Image *</label>
            <input type="file" ref={fileInputRef} accept="image/*" className="form-input" required />
          </div>
          <div className="form-group">
            <label className="form-label">UPI Display Label *</label>
            <input
              type="text"
              className="form-input"
              placeholder='e.g., "Pay to: Dhan-Gaadi Fleet UPI"'
              value={upiLabel}
              onChange={(e) => setUpiLabel(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label className="form-label">UPI ID (optional)</label>
            <input
              type="text"
              className="form-input"
              placeholder="e.g., hroadways@upi"
              value={upiId}
              onChange={(e) => setUpiId(e.target.value)}
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={uploading}>
            {uploading ? 'Uploading...' : '📤 Upload QR Code'}
          </button>
        </form>
      </div>

      {/* QR Code List */}
      <h3 className="heading-4" style={{ margin: '2rem 0 1rem' }}>Uploaded QR Codes</h3>
      {qrCodes.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">📱</span>
          <p style={{ color: 'var(--text-secondary)' }}>No QR codes uploaded yet</p>
        </div>
      ) : (
        <div className={styles.qrGrid}>
          {qrCodes.map((qr) => (
            <div key={qr._id} className={`${styles.qrCard} ${qr.isActive ? styles.qrActive : ''}`}>
              <div className={styles.qrImage}>
                <img src={buildAssetUrl(qr.imagePath)} alt={qr.upiLabel} />
              </div>
              <div className={styles.qrInfo}>
                <strong>{qr.upiLabel}</strong>
                {qr.upiId && <span>{qr.upiId}</span>}
                <span className={`badge ${qr.isActive ? 'badge-success' : 'badge-neutral'}`}>
                  {qr.isActive ? 'ACTIVE' : 'INACTIVE'}
                </span>
              </div>
              <div className={styles.qrActions}>
                <button
                  className={`btn ${qr.isActive ? 'btn-ghost' : 'btn-primary'} btn-sm`}
                  onClick={() => handleToggle(qr._id)}
                  disabled={qr.isActive}
                >
                  {qr.isActive ? 'Active' : 'Activate'}
                </button>
                <button className="btn btn-danger btn-sm" onClick={() => handleDelete(qr._id)}>
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
