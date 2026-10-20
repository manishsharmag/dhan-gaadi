'use client';
import { use, Suspense } from 'react';
import BookingPage from '../page';

function BookingContent({ params }) {
  const unwrappedParams = typeof params?.then === 'function' ? use(params) : params;
  return <BookingPage routeScheduleId={unwrappedParams?.scheduleId} />;
}

export default function DynamicBookingPage({ params }) {
  return (
    <Suspense fallback={<div className="loading-overlay" style={{ minHeight: '60vh' }}><div className="spinner" /></div>}>
      <BookingContent params={params} />
    </Suspense>
  );
}
