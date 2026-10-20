'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Navbar from '@/components/Navbar';
import { useAuth } from '@/lib/auth';
import { authAPI } from '@/lib/api';
import { isAdminRole } from '@/lib/config';
import styles from './login.module.css';

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [tab, setTab] = useState('login'); // 'login' or 'register'
  const [mobile, setMobile] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleAuth = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (tab === 'login') {
        const res = await authAPI.login(mobile, password);
        const { token, user } = res.data;
        login(token, user);
        const nextPath = typeof window !== 'undefined'
          ? new URLSearchParams(window.location.search).get('next')
          : null;
        router.push(nextPath || (isAdminRole(user.role) ? '/admin' : '/'));
      } else {
        const res = await authAPI.register(mobile, name, password);
        const { token, user } = res.data;
        login(token, user);
        router.push('/');
      }
    } catch (err) {
      setError(err.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Navbar />
      <main className={styles.page}>
        <div className={styles.loginContainer}>
          <div className={styles.loginCard}>
            <div className={styles.loginHeader}>
              <span className={styles.loginIcon}>🚌</span>
              <h1 className="heading-2">Welcome</h1>
              <p className="text-body" style={{ color: 'var(--text-secondary)' }}>
                {tab === 'login' ? 'Log in to your account' : 'Create a new account'}
              </p>
            </div>

            <div style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
                <button 
                  className={`btn ${tab === 'login' ? 'btn-primary' : 'btn-outline'} btn-full`}
                  onClick={() => { setTab('login'); setError(''); }}
                >
                  Login
                </button>
                <button 
                  className={`btn ${tab === 'register' ? 'btn-primary' : 'btn-outline'} btn-full`}
                  onClick={() => { setTab('register'); setError(''); }}
                >
                  Sign Up
                </button>
            </div>

            {error && (
              <div className={styles.alert} style={{ background: 'var(--danger-light)', color: '#991b1b', marginBottom: '15px', padding: '10px', borderRadius: '8px' }}>
                ❌ {error}
              </div>
            )}
            {success && (
              <div className={styles.alert} style={{ background: 'var(--success-light)', color: '#065f46', marginBottom: '15px', padding: '10px', borderRadius: '8px' }}>
                ✅ {success}
              </div>
            )}

            <form onSubmit={handleAuth} className={styles.loginForm}>
              {tab === 'register' && (
                <div className="form-group" style={{ marginBottom: '15px' }}>
                  <label className="form-label" htmlFor="name">Full Name</label>
                  <input
                    id="name"
                    type="text"
                    className="form-input"
                    placeholder="Enter your name"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required={tab === 'register'}
                  />
                </div>
              )}
              
              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label className="form-label" htmlFor="mobile">Mobile Number</label>
                <div className={styles.phoneInput}>
                  <span className={styles.phonePrefix}>+91</span>
                  <input
                    id="mobile"
                    type="tel"
                    className="form-input"
                    placeholder="10-digit mobile number"
                    value={mobile}
                    onChange={(e) => setMobile(e.target.value.replace(/\D/g, '').slice(0, 10))}
                    pattern="[6-9][0-9]{9}"
                    maxLength={10}
                    required
                    style={{ borderTopLeftRadius: 0, borderBottomLeftRadius: 0 }}
                  />
                </div>
              </div>

              <div className="form-group" style={{ marginBottom: '20px' }}>
                <label className="form-label" htmlFor="password">Password</label>
                <input
                  id="password"
                  type="password"
                  className="form-input"
                  placeholder="Enter your password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  minLength={6}
                  required
                />
              </div>

              <button type="submit" className="btn btn-primary btn-lg btn-full" disabled={loading || mobile.length !== 10 || password.length < 6}>
                {loading ? 'Processing...' : (tab === 'login' ? 'Login →' : 'Create Account →')}
              </button>
            </form>

            <p className={styles.terms}>
              By continuing, you agree to Dhan-Gaadi Fleet OS terms of service.
            </p>
          </div>
        </div>
      </main>
    </>
  );
}
