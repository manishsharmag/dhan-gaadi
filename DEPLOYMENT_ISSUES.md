# Production Deployment Troubleshooting Guide

## Deployment Issues & Solutions

### 1. Fare Showing 0 When Booking Buses

**Problem:** After deploying backend to Hostinger, demo data loads but ticket fare shows `0`.

**Root Causes:**
- Schedules collection missing `fare` field
- Auto-seed script only runs on empty database (first deployment)
- Existing schedules created before `fare` field was added to schema

**Solution:**

Run the seed-atlas script on your backend:

```bash
cd server
npm install
node seed-atlas.js
```

**Required Environment Variables:**
```
MONGO_URI=mongodb+srv://username:password@cluster.mongodb.net/dhan-gaadi
```

**What the script does:**
- Creates demo users (owner, admin, passenger, conductor)
- Creates 5 demo buses with seat capacity (40-52 seats)
- Creates 5 routes with base fares (200-450)
- Creates 10 schedules with explicit fare values (200-650)
- Ensures all existing schedules have fare populated
- Creates default UPI QR code for payments

**If you already have data:**
The script checks if collections exist and only updates schedules without fare values, so it won't duplicate data.

---

### 2. HTTP 500 Error on Booking Confirmation

**Problem:** After selecting seats, entering passenger details, and clicking "Confirm & Proceed to Pay" → HTTP 500 error.

**Likely Causes:**
1. Seat locking system failing (SeatLock model/collection issue)
2. Schedule data incomplete or corrupt
3. Server error not being logged properly

**Debugging Steps:**

1. **Check server logs** on your Hostinger backend:
   ```bash
   # SSH into Hostinger
   pm2 logs dhan-gaadi  # or your app name
   # Look for [BOOKING] errors
   ```

2. **Verify schedule data:**
   ```bash
   # In MongoDB Atlas UI:
   # Go to Collections → schedules
   # Check if records have: route, bus, fare, departureTime, daysOfOperation
   ```

3. **Common fixes:**
   - Ensure `schedule.fare` is NOT null/0
   - Ensure `schedule.route` is properly populated (should be ObjectId referencing Route)
   - Ensure `schedule.bus` is properly populated
   - Run seed-atlas.js again to ensure valid data

4. **Force reseed if corruption suspected:**
   ```bash
   # In MongoDB Atlas UI:
   # Delete schedules, seatLocks collections
   # Then run: node seed-atlas.js
   ```

---

### 3. No Schedules Available in Profile & POS Screens

**Problem:** Profile page and POS screen show "No schedules available" even though demo data exists.

**Root Causes:**
1. Schedules not seeded (see Issue #1)
2. Date filtering doesn't match `daysOfOperation`
3. Schedule status is `inactive` instead of `active`

**Debugging:**

1. **Check schedule count:**
   ```bash
   curl "http://your-backend:8525/api/schedules"
   ```

2. **Verify daysOfOperation matches:**
   - Schedules have array like `["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]`
   - Or `["Mon", "Tue", "Wed", "Thu", "Fri"]` for weekdays only
   - Today's day must be in this array

3. **Check status field:**
   - In MongoDB, verify `status: "active"` (not "inactive" or missing)

4. **Solution:**
   - Run seed-atlas.js to create valid schedules
   - Schedules will have `daysOfOperation: all days` and `status: "active"`

---

### 4. Android QR Scanner & ZXing Integration

**Architecture:** Barcode and QR scanning is implemented in `AdminDashboardScreen.kt` via the ZXing `ScanContract()` and `ScanOptions()`.

**Implementation Details:**
- Uses `rememberLauncherForActivityResult(contract = ScanContract())`
- Launches dedicated QR scanner with prompt `Scan passenger's e-Ticket QR code`
- Automatically extracts PNR from raw text, JSON payload (`{"pnr":"..."}`), or URL routes (`/ticket/{pnr}`) via `extractTicketPnr`
- Automatically checks verification status with backend API via `adminViewModel.scanTicket(pnr)`

**For Users:**

1. **First time using scanner:**
   - App will either show permission dialog or "Camera Permission Required" screen
   - Tap "Go Back" and grant permission in Settings → Apps → Haryana Roadways → Permissions

2. **If already granted but still crashes:**
   - Settings → Apps → Haryana Roadways → Permissions → Camera
   - Toggle OFF then ON
   - Reinstall app if still fails

3. **Debug logs (if rebuilt):**
   - Connect Android device: `adb logcat | grep ScannerScreen`
   - Look for camera initialization errors

**Rebuild APK:**
```bash
cd android
./gradlew assembleRelease  # Creates: app/build/outputs/apk/release/app-release.apk
```

---

### 5. MongoDB Atlas Setup with Hostinger Backend

**Network Architecture:**
```
Android App / Web Client
    ↓
Hostinger Backend (Node.js/Express)
    ↓
MongoDB Atlas (MongoDB cluster in cloud)
    ↓
Hostinger IP whitelist
```

**Setup Steps:**

#### 1. MongoDB Atlas Configuration

```
1. Create cluster at https://www.mongodb.com/products/platform/atlas
2. Get connection string (Database → Connect):
   mongodb+srv://username:password@cluster.mongodb.net/dhan-gaadi
3. Add Hostinger IP to IP Whitelist:
   - Get server IP from Hostinger cPanel
   - Go to Atlas → Network Access → IP Whitelist
   - Add: your-hostinger-ip/32
4. Create database user with strong password
```

#### 2. Hostinger Backend Configuration

```bash
# SSH into Hostinger server
nano .env

# Add:
MONGO_URI=mongodb+srv://username:password@cluster-name.mongodb.net/dhan-gaadi
JWT_SECRET=your-secret-key-here
NODE_ENV=production

# Save and restart app
pm2 restart dhan-gaadi
pm2 save
```

#### 3. Seed Database

```bash
# Run seed script
node server/seed-atlas.js

# Or add to package.json:
"scripts": {
  "seed": "node server/seed-atlas.js"
}

# Then:
npm run seed
```

#### 4. Verify Connection

```bash
# Test from local machine
curl http://your-hostinger-backend.com/api/schedules

# Should return array of schedules with fare > 0
```

**Common Issues:**

| Issue | Solution |
|-------|----------|
| Connection timeout | Increase timeout to 300s+ in connection string; check Hostinger IP whitelist |
| 403 Unauthorized | Verify username/password in MONGO_URI |
| No data after seed | Check if seed-atlas.js ran successfully; verify MONGO_URI in .env |
| Slow queries | Add database indexes; enable read preference for scaling |

---

## Testing Checklist

After all fixes:

- [ ] **Fare Display:** Book a route → fare shows correct amount (not 0)
- [ ] **Booking Complete:** Click confirm & pay → no HTTP 500, shows payment screen
- [ ] **Profile Tickets:** Profile page shows available schedules and past bookings
- [ ] **POS Tickets:** POS can select schedule to generate tickets
- [ ] **QR Scanner:** Click "Scan" → shows camera preview or permission request (not crash)
- [ ] **Data Seed:** Run `node seed-atlas.js` → shows "ATLAS SEED COMPLETE"

---

## Support Commands

```bash
# Check backend status
curl http://your-backend/api/schedules

# View seed script output
npm run seed 2>&1 | tee seed.log

# Check database size
# In MongoDB Atlas UI → Metrics

# View recent logs
# pm2 logs dhan-gaadi --lines 100

# Manually test booking
curl -X POST http://your-backend/api/bookings \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "scheduleId": "SCHEDULE_ID",
    "journeyDate": "2026-07-28",
    "seats": [1],
    "passengerName": "Test",
    "passengerMobile": "9876543210"
  }'
```

---

## Next Steps

1. **Run seed-atlas.js** on Hostinger backend
2. **Test booking flow** in browser/app
3. **Check scheduler** for any errors
4. **Rebuild Android APK** if using new code (has permission fix)
5. **Monitor logs** for any remaining 500 errors

If issues persist, share:
- Backend error logs (pm2 logs output)
- MongoDB Atlas screenshots (schedules data)
- Android logcat output (if crash)
