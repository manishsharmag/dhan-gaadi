import Link from 'next/link';
import styles from './Footer.module.css';

export default function Footer() {
  return (
    <footer className={styles.footer}>
      <div className={`container ${styles.footerInner}`}>
        <div className={styles.footerGrid}>
          <div className={styles.footerCol}>
            <div className={styles.footerLogo}>
              <span>🚌</span>
              <div>
                <strong>Dhan-Gaadi Fleet OS</strong>
                <p>Enterprise Intercity Transit & Ground Mobility</p>
              </div>
            </div>
            <p className={styles.footerDesc}>
              Next-generation online fleet reservation, live telemetry tracking, and verified QR passes for state-of-the-art highway passenger transportation.
            </p>
          </div>
          
          <div className={styles.footerCol}>
            <h4>Popular Routes</h4>
            <ul>
              <li>Hisar → Chandigarh</li>
              <li>Hisar → Delhi</li>
              <li>Ambala → Delhi</li>
              <li>Rohtak → Chandigarh</li>
              <li>Hisar → Rohtak</li>
            </ul>
          </div>
          
          <div className={styles.footerCol}>
            <h4>Quick Links</h4>
            <ul>
              <li><Link href="/search">Search Routes</Link></li>
              <li><Link href="/ticket">Find e-Ticket</Link></li>
              <li><Link href="/track">Live Fleet Radar</Link></li>
              <li><Link href="/profile">My Bookings</Link></li>
              <li><a href="/brochure/index.html" target="_blank" rel="noopener noreferrer" style={{ color: '#f59e0b', fontWeight: 600 }}>📊 Commercial Deck & ROI</a></li>
            </ul>
          </div>
          
          <div className={styles.footerCol}>
            <h4>Contact</h4>
            <ul>
              <li>📞 24x7 Helpline: 1800-180-2057</li>
              <li>📧 fleet-ops@dhangaadi.com</li>
              <li>🏢 Dhan-Gaadi Fleet Mobility OS</li>
            </ul>
          </div>
        </div>
        
        <div className={styles.footerBottom}>
          <p>© {new Date().getFullYear()} Dhan-Gaadi Fleet OS. All rights reserved.</p>
          <p>Enterprise Ground Mobility Infrastructure</p>
        </div>
      </div>
    </footer>
  );
}
