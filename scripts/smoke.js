(async () => {
  try {
    const fetch = globalThis.fetch;
    // Login as admin / owner
    const mobile = process.env.SMOKE_TEST_MOBILE || '9999999999';
    const password = process.env.SMOKE_TEST_PASSWORD || 'password123';
    const loginRes = await fetch('http://localhost:8525/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ mobile, password }),
    });
    const login = await loginRes.json();
    console.log('Login:', login.message);
    const token = login.data?.token;
    if (!token) { console.error('No token'); return; }

    // Get schedules
    const schedulesRes = await fetch('http://localhost:8525/api/schedules');
    const schedules = await schedulesRes.json();
    const schedule = schedules.data && schedules.data[0];
    if (!schedule) { console.error('No schedules'); return; }
    console.log('Using schedule:', schedule._id);

    const date = new Date().toISOString().split('T')[0];
    const seatsRes = await fetch(`http://localhost:8525/api/bookings/seats?scheduleId=${schedule._id}&date=${date}`);
    const seatsJson = await seatsRes.json();
    console.log('Unavailable seats:', seatsJson.data?.unavailableSeats?.slice(0,20));

    // Find first available seat between 1..totalSeats
    const total = schedule.bus?.totalSeats || 45;
    const unavailable = new Set(seatsJson.data?.unavailableSeats || []);
    let pick = null;
    for (let i = 1; i <= total; i++) {
      if (!unavailable.has(i)) { pick = i; break; }
    }
    if (!pick) { console.error('No free seats'); return; }
    console.log('Picking seat:', pick);

    const bookingBody = {
      scheduleId: schedule._id,
      journeyDate: date,
      seats: [pick],
      passengerName: 'Smoke Test',
      passengerMobile: '9876500000',
    };

    const bookRes = await fetch('http://localhost:8525/api/bookings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify(bookingBody),
    });
    const bookJson = await bookRes.json();
    console.log('Booking response:', bookJson.message);
    console.log(JSON.stringify(bookJson.data, null, 2));
  } catch (err) {
    console.error('Smoke script error', err);
  }
})();
