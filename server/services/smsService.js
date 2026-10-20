const https = require('https');

/**
 * Send SMS via Fast2SMS API
 * In development mode, logs to console instead of sending real SMS
 */
class SMSService {
  constructor() {
    this.apiKey = process.env.FAST2SMS_API_KEY;
    this.baseUrl = 'www.fast2sms.com';
  }

  _getPrimaryFrontendUrl() {
    return (process.env.FRONTEND_URL || 'https://haryanabusinfo.in')
      .split(',')[0]
      .trim()
      .replace(/\/+$/, '');
  }

  async sendOTP(mobile, otp) {
    const message = `Your Dhan-Gaadi verification code is: ${otp}. Valid for 5 minutes. Do not share.`;
    return this._send(mobile, message);
  }

  async sendBookingConfirmation(mobile, data) {
    const message = `[Dhan-Gaadi Fleet OS] Booking ${data.referenceCode} is CONFIRMED. Seats: ${data.seats.join(',')}. PNR: ${data.pnr}. e-Ticket: ${data.ticketUrl}`;
    return this._send(mobile, message);
  }

  async sendBookingRejection(mobile, data) {
    const frontendUrl = this._getPrimaryFrontendUrl();
    const message = `[Dhan-Gaadi] Payment verification pending/not confirmed for ${data.referenceCode}. Seats released. Please book again at ${frontendUrl}`;
    return this._send(mobile, message);
  }

  async _send(mobile, message) {
    const cleanMobile = String(mobile || '').replace(/\D/g, '').slice(-10);
    if (!cleanMobile) return { success: false, error: 'Invalid mobile number' };

    // In development, just log
    if (process.env.NODE_ENV !== 'production' || !this.apiKey) {
      console.log(`[SMS DEV] To: ${cleanMobile} | Message: ${message}`);
      return { success: true, dev: true };
    }

    return new Promise((resolve, reject) => {
      const data = JSON.stringify({
        route: 'q', // Quick SMS route
        message,
        language: 'english',
        flash: 0,
        numbers: cleanMobile,
      });

      const options = {
        hostname: this.baseUrl,
        path: '/dev/bulkV2',
        method: 'POST',
        headers: {
          'authorization': this.apiKey,
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(data),
        },
      };

      const req = https.request(options, (res) => {
        let body = '';
        res.on('data', chunk => body += chunk);
        res.on('end', () => {
          try {
            const parsed = JSON.parse(body);
            resolve(parsed);
          } catch (e) {
            resolve({ success: true, raw: body });
          }
        });
      });

      req.on('error', (e) => {
        console.error('[SMS ERROR]', e.message);
        // Don't fail the booking if SMS fails
        resolve({ success: false, error: e.message });
      });

      req.write(data);
      req.end();
    });
  }
}

module.exports = new SMSService();
