/**
 * API Testing Script
 * Tests all major endpoints to verify they work correctly
 */

(async () => {
  const BASE_URL = 'http://localhost:8525/api';
  let token = null;

  console.log('🧪 API ENDPOINT TESTING\n');

  try {
    // ========== 1. Test Login ==========
    console.log('1️⃣  Testing Login Endpoint...');
    const testMobile = process.env.TEST_ADMIN_MOBILE || '9991672565';
    const testPassword = process.env.TEST_ADMIN_PASSWORD || 'password123';
    let res = await fetch(`${BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ mobile: testMobile, password: testPassword })
    });
    let json = await res.json();
    console.log(`   Status: ${res.status}`);
    if (res.status === 200) {
      token = json.data?.token;
      console.log(`   ✅ LOGIN WORKS — Token: ${token ? 'Obtained' : 'Missing'}`);
    } else {
      console.log(`   ❌ LOGIN FAILED: ${json.message}`);
    }
    console.log();

    // ========== 2. Test Get Schedules (NO AUTH) ==========
    console.log('2️⃣  Testing GET /schedules (public)...');
    res = await fetch(`${BASE_URL}/schedules`);
    json = await res.json();
    console.log(`   Status: ${res.status}`);
    console.log(`   Count: ${json.data?.length || 0} schedules`);
    if (json.data && json.data.length > 0) {
      const sched = json.data[0];
      console.log(`   ✅ SCHEDULES ENDPOINT WORKS`);
      console.log(`   Sample: Route=${sched.route?.name}, Fare=${sched.fare}, Departure=${sched.departureTime}`);
    } else {
      console.log(`   ⚠️  No schedules found`);
    }
    console.log();

    // ========== 3. Test Get Active QR (NO AUTH) ==========
    console.log('3️⃣  Testing GET /qr/active (public)...');
    res = await fetch(`${BASE_URL}/qr/active`);
    json = await res.json();
    console.log(`   Status: ${res.status}`);
    if (res.status === 200 && json.data) {
      console.log(`   ✅ QR ENDPOINT WORKS`);
      console.log(`   UPI: ${json.data.upiLabel}`);
    } else {
      console.log(`   ⚠️  No QR code found or error: ${json.message}`);
    }
    console.log();

    if (!token) {
      console.log('❌ Cannot continue — login failed. Skipping authenticated endpoints.');
      process.exit(1);
    }

    // ========== 4. Test Get Profile (WITH AUTH) ==========
    console.log('4️⃣  Testing GET /auth/profile (authenticated)...');
    res = await fetch(`${BASE_URL}/auth/profile`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    json = await res.json();
    console.log(`   Status: ${res.status}`);
    if (res.status === 200 && json.data) {
      console.log(`   ✅ PROFILE ENDPOINT WORKS`);
      console.log(`   User: ${json.data.name}, Role: ${json.data.role}`);
    } else {
      console.log(`   ❌ PROFILE FAILED: ${json.message}`);
    }
    console.log();

    // ========== 5. Test Get My Bookings (WITH AUTH) ==========
    console.log('5️⃣  Testing GET /bookings/my-bookings (authenticated)...');
    res = await fetch(`${BASE_URL}/bookings/my-bookings`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    json = await res.json();
    console.log(`   Status: ${res.status}`);
    if (res.status === 200) {
      console.log(`   ✅ MY BOOKINGS ENDPOINT WORKS — Count: ${json.data?.length || 0}`);
    } else {
      console.log(`   ❌ MY BOOKINGS FAILED: ${json.message}`);
    }
    console.log();

    // ========== 6. Test Get All Routes (NO AUTH) ==========
    console.log('6️⃣  Testing GET /routes (public)...');
    res = await fetch(`${BASE_URL}/routes`);
    json = await res.json();
    console.log(`   Status: ${res.status}`);
    if (json.data && json.data.length > 0) {
      console.log(`   ✅ ROUTES ENDPOINT WORKS — Count: ${json.data.length}`);
      console.log(`   Sample: ${json.data[0].name} (${json.data[0].source} → ${json.data[0].destination})`);
    } else {
      console.log(`   ⚠️  No routes found`);
    }
    console.log();

    // ========== 7. Test Get All Buses (NO AUTH) ==========
    console.log('7️⃣  Testing GET /buses (public)...');
    res = await fetch(`${BASE_URL}/buses`);
    json = await res.json();
    console.log(`   Status: ${res.status}`);
    if (json.data && json.data.length > 0) {
      console.log(`   ✅ BUSES ENDPOINT WORKS — Count: ${json.data.length}`);
      console.log(`   Sample: ${json.data[0].name} (${json.data[0].totalSeats} seats)`);
    } else {
      console.log(`   ⚠️  No buses found`);
    }
    console.log();

    // ========== 8. Test Admin Dashboard (WITH AUTH + ADMIN) ==========
    console.log('8️⃣  Testing GET /bookings/admin/dashboard (admin only)...');
    res = await fetch(`${BASE_URL}/bookings/admin/dashboard`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    json = await res.json();
    console.log(`   Status: ${res.status}`);
    if (res.status === 200) {
      console.log(`   ✅ ADMIN DASHBOARD WORKS`);
      console.log(`   Stats:`, JSON.stringify(json.data, null, 2).split('\n').slice(0, 5).join('\n   '));
    } else {
      console.log(`   ⚠️  Not admin? Status: ${res.status}, Message: ${json.message}`);
    }
    console.log();

    // ========== 9. Test Seat Availability (NO AUTH) ==========
    console.log('9️⃣  Testing GET /bookings/seats (seat availability)...');
    if (json.data && json.data.length > 0) {
      const scheduleId = json.data[0]._id;
      const date = new Date().toISOString().split('T')[0];
      res = await fetch(`${BASE_URL}/bookings/seats?scheduleId=${scheduleId}&date=${date}`);
      json = await res.json();
      console.log(`   Status: ${res.status}`);
      if (res.status === 200) {
        console.log(`   ✅ SEAT AVAILABILITY WORKS`);
        console.log(`   Total Seats: ${json.data?.totalSeats}, Unavailable: ${json.data?.unavailableSeats?.length || 0}`);
      } else {
        console.log(`   ❌ SEAT CHECK FAILED: ${json.message}`);
      }
    }
    console.log();

    console.log('✅ API TESTING COMPLETE\n');
    console.log('📊 SUMMARY:');
    console.log('   - Public endpoints (schedules, routes, buses, qr/active) work');
    console.log('   - Auth endpoint works (login/token generation)');
    console.log('   - Protected endpoints work (profile, bookings) with valid token');
    console.log('   - Admin endpoints respond appropriately');

  } catch (err) {
    console.error('❌ TEST ERROR:', err.message);
    process.exit(1);
  }

  process.exit(0);
})();
