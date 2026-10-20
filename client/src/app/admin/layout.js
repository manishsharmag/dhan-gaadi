'use client';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth';
import AdminSidebar from '@/components/AdminSidebar';
import styles from './admin.module.css';

export default function AdminLayout({ children }) {
  const router = useRouter();
  const { user, isAuthenticated, isAdmin, loading } = useAuth();

  useEffect(() => {
    if (!loading && (!isAuthenticated || !isAdmin)) {
      const next = encodeURIComponent(window.location.pathname + window.location.search);
      router.push(`/login?next=${next}`);
    }
  }, [loading, isAuthenticated, isAdmin, router]);

  if (loading) {
    return <div className="loading-overlay" style={{ minHeight: '100vh' }}><div className="spinner" /></div>;
  }

  if (!isAdmin) return null;

  return (
    <div className={styles.adminLayout}>
      <AdminSidebar />
      <main className={styles.adminMain}>
        {children}
      </main>
    </div>
  );
}
