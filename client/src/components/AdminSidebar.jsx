'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '@/lib/auth';
import styles from './AdminSidebar.module.css';

const NAV_ITEMS = [
  { href: '/admin', label: 'Dashboard', icon: '📊' },
  { href: '/admin/bookings', label: 'Verification Queue', icon: '✅' },
  { href: '/admin/qr', label: 'QR Code', icon: '📱' },
  { href: '/admin/conductors', label: 'Conductors / Staff', icon: '👥' },
  { href: '/admin/routes', label: 'Routes', icon: '🛣️' },
  { href: '/admin/buses', label: 'Buses', icon: '🚌' },
  { href: '/admin/schedules', label: 'Schedules', icon: '📅' },
  { href: '/admin/reports', label: 'Reports', icon: '📈' },
];

export default function AdminSidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  return (
    <aside className={styles.sidebar}>
      {/* Logo */}
      <div className={styles.logo}>
        <Link href="/admin">
          <span className={styles.logoIcon}>🚌</span>
          <div>
            <strong>Dhan-Gaadi Admin</strong>
            <span>Fleet OS Console</span>
          </div>
        </Link>
      </div>

      {/* Nav */}
      <nav className={styles.nav}>
        {NAV_ITEMS.map((item) => {
          const isActive = pathname === item.href || 
            (item.href !== '/admin' && pathname.startsWith(item.href));
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`${styles.navItem} ${isActive ? styles.active : ''}`}
            >
              <span className={styles.navIcon}>{item.icon}</span>
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>

      {/* Bottom */}
      <div className={styles.sidebarBottom}>
        <Link href="/" className={styles.navItem}>
          <span className={styles.navIcon}>🌐</span>
          <span>View Site</span>
        </Link>
        <div className={styles.userInfo}>
          <div className={styles.userAvatar}>
            {user?.name?.[0]?.toUpperCase() || 'A'}
          </div>
          <div className={styles.userDetails}>
            <strong>{user?.name || 'Admin'}</strong>
            <span>{user?.mobile}</span>
          </div>
        </div>
        <button onClick={logout} className={styles.logoutBtn}>
          Logout
        </button>
      </div>
    </aside>
  );
}
