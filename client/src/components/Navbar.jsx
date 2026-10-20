'use client';
import { useState } from 'react';
import Link from 'next/link';
import { useAuth } from '@/lib/auth';
import styles from './Navbar.module.css';

export default function Navbar() {
  const { user, isAdmin, isAuthenticated, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <nav className={styles.nav}>
      <div className={`container ${styles.navInner}`}>
        {/* Logo */}
        <Link href="/" className={styles.logo}>
          <img 
            src="/logo.png" 
            alt="Dhan-Gaadi Official Logo" 
            style={{ width: '40px', height: '40px', borderRadius: '50%', objectFit: 'cover', border: '2px solid var(--primary-light, #0d9488)', flexShrink: 0 }} 
          />
          <div className={styles.logoText}>
            <span className={styles.logoTitle}>Dhan-Gaadi</span>
            <span className={styles.logoSub}>Fleet OS • Ground Mobility</span>
          </div>
        </Link>

        {/* Desktop Nav */}
        <div className={styles.desktopNav}>
          <Link href="/" className={styles.navLink}>Home</Link>
          <Link href="/search" className={styles.navLink}>Search Routes</Link>
          <Link href="/ticket" className={styles.navLink}>Find Ticket</Link>
          <Link href="/track" className={`${styles.navLink} ${styles.liveFleetLink}`}>
            <span className={styles.pulseDot} />
            Live Tracking
          </Link>
          <a href="/brochure/index.html" target="_blank" rel="noopener noreferrer" className={styles.navLink} style={{ color: '#f59e0b', fontWeight: 600 }}>
            Prospectus
          </a>
          {isAuthenticated ? (
            <>
              <Link href="/profile" className={styles.navLink}>🎫 My Bookings</Link>
              {isAdmin && (
                <Link href="/admin" className={styles.navLinkAdmin}>Admin Control</Link>
              )}
              <div className={styles.userMenu}>
                <div className={styles.userInfo}>
                  <span className={styles.userAvatar}>
                    {user?.name?.[0]?.toUpperCase() || user?.mobile?.slice(-2) || 'U'}
                  </span>
                  <span className={styles.userName}>{user?.name || user?.mobile}</span>
                </div>
                <button onClick={logout} className="btn btn-secondary btn-sm" aria-label="Sign out">
                  Logout
                </button>
              </div>
            </>
          ) : (
            <Link href="/login" className="btn btn-primary btn-sm">
              Sign In / Register
            </Link>
          )}
        </div>

        {/* Mobile hamburger */}
        <button 
          className={styles.hamburger} 
          onClick={() => setMenuOpen(!menuOpen)}
          aria-label="Toggle navigation menu"
          aria-expanded={menuOpen}
        >
          <span className={`${styles.hamburgerLine} ${menuOpen ? styles.open : ''}`} />
          <span className={`${styles.hamburgerLine} ${menuOpen ? styles.open : ''}`} />
          <span className={`${styles.hamburgerLine} ${menuOpen ? styles.open : ''}`} />
        </button>
      </div>

      {/* Mobile Menu */}
      {menuOpen && (
        <div className={styles.mobileMenu}>
          <Link href="/" className={styles.mobileLink} onClick={() => setMenuOpen(false)}>🏠 Home</Link>
          <Link href="/search" className={styles.mobileLink} onClick={() => setMenuOpen(false)}>🔍 Search Routes</Link>
          <Link href="/ticket" className={styles.mobileLink} onClick={() => setMenuOpen(false)}>🎫 Find e-Ticket</Link>
          <Link href="/track" className={`${styles.mobileLink} ${styles.liveFleetLink}`} onClick={() => setMenuOpen(false)}>
            <span className={styles.pulseDot} />
            Live Bus Tracking
          </Link>
          <a href="/brochure/index.html" target="_blank" rel="noopener noreferrer" className={styles.mobileLink} style={{ color: '#f59e0b', fontWeight: 600 }} onClick={() => setMenuOpen(false)}>
            📊 Commercial Prospectus
          </a>
          {isAuthenticated ? (
            <>
              <Link href="/profile" className={styles.mobileLink} onClick={() => setMenuOpen(false)}>👤 My Bookings</Link>
              {isAdmin && (
                <Link href="/admin" className={`${styles.mobileLink} ${styles.mobileAdminLink}`} onClick={() => setMenuOpen(false)}>
                  ⚙️ Admin Control Panel
                </Link>
              )}
              <div className={styles.mobileAuthRow}>
                <span className={styles.mobileUserLabel}>Signed in as {user?.name || user?.mobile}</span>
                <button onClick={() => { logout(); setMenuOpen(false); }} className="btn btn-secondary btn-full">
                  Logout
                </button>
              </div>
            </>
          ) : (
            <Link href="/login" className="btn btn-primary btn-full" onClick={() => setMenuOpen(false)}>
              Sign In / Register
            </Link>
          )}
        </div>
      )}
    </nav>
  );
}
