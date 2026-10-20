import './globals.css';
import { AuthProvider } from '@/lib/auth';
import { ToastProvider } from '@/components/Toast';

export const metadata = {
  title: 'Dhan-Gaadi Fleet OS — Enterprise Bus Booking & Intercity Transit',
  description: 'Next-generation online bus ticket booking and fleet management system. Book seats, track live telemetry, and get verified digital passes across major highway routes.',
  keywords: 'Dhan-Gaadi, bus booking, fleet OS, intercity transit, online bus ticket, live bus tracking',
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          <ToastProvider>
            {children}
          </ToastProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
