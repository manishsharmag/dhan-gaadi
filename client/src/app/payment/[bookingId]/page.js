'use client';
import { use, Suspense } from 'react';
import PaymentPage from '../page';

function PaymentContent({ params }) {
  const unwrappedParams = typeof params?.then === 'function' ? use(params) : params;
  return <PaymentPage routeBookingId={unwrappedParams?.bookingId} />;
}

export default function DynamicPaymentPage({ params }) {
  return (
    <Suspense fallback={<div className="loading-overlay" style={{ minHeight: '60vh' }}><div className="spinner" /></div>}>
      <PaymentContent params={params} />
    </Suspense>
  );
}
