import Link from 'next/link';
import Navbar from '@/components/Navbar';
import Footer from '@/components/Footer';

export default function NotFound() {
  return (
    <>
      <Navbar />
      <main style={{
        minHeight: '70vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '3rem 1.5rem',
        background: 'var(--bg-primary, #0B0F17)',
        color: 'var(--text-primary, #F8FAFC)'
      }}>
        <div style={{
          maxWidth: '520px',
          textAlign: 'center',
          background: 'rgba(255, 255, 255, 0.03)',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          borderRadius: '24px',
          padding: '3rem 2rem',
          backdropFilter: 'blur(16px)',
          boxShadow: '0 20px 40px rgba(0,0,0,0.4)'
        }}>
          <div style={{
            width: '72px',
            height: '72px',
            borderRadius: '20px',
            background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.2), rgba(6, 78, 59, 0.4))',
            border: '1px solid rgba(52, 211, 153, 0.3)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '2rem',
            margin: '0 auto 1.5rem'
          }}>
            🚌
          </div>

          <span style={{
            fontSize: '0.85rem',
            fontWeight: 700,
            letterSpacing: '0.1em',
            textTransform: 'uppercase',
            color: '#34D399',
            background: 'rgba(52, 211, 153, 0.1)',
            padding: '0.3rem 0.8rem',
            borderRadius: '999px',
            display: 'inline-block',
            marginBottom: '1rem'
          }}>
            404 Route Not Found
          </span>

          <h1 style={{
            fontSize: '2rem',
            fontWeight: 800,
            marginBottom: '0.75rem',
            letterSpacing: '-0.02em',
            color: '#FFFFFF'
          }}>
            Destination Off The Grid
          </h1>

          <p style={{
            color: '#94A3B8',
            fontSize: '1rem',
            lineHeight: 1.6,
            marginBottom: '2rem'
          }}>
            The route or page you are trying to reach does not exist or has been rescheduled on the Dhan-Gaadi network.
          </p>

          <div style={{
            display: 'flex',
            gap: '1rem',
            justifyContent: 'center',
            flexWrap: 'wrap'
          }}>
            <Link
              href="/"
              className="btn btn-primary"
              style={{
                background: 'linear-gradient(135deg, #059669 0%, #10B981 100%)',
                padding: '0.75rem 1.5rem',
                borderRadius: '12px',
                fontWeight: 600,
                textDecoration: 'none',
                color: '#FFFFFF'
              }}
            >
              🏠 Return to Home
            </Link>
            <Link
              href="/search"
              className="btn btn-outline"
              style={{
                border: '1px solid rgba(255, 255, 255, 0.2)',
                padding: '0.75rem 1.5rem',
                borderRadius: '12px',
                fontWeight: 600,
                textDecoration: 'none',
                color: '#F8FAFC'
              }}
            >
              🔍 Search Buses
            </Link>
          </div>
        </div>
      </main>
      <Footer />
    </>
  );
}
