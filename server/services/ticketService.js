const PDFDocument = require('pdfkit');
const QRCode = require('qrcode');
const fs = require('fs');
const path = require('path');

const TICKETS_DIR = path.join(__dirname, '..', 'public', 'tickets');

// Ensure tickets directory exists
if (!fs.existsSync(TICKETS_DIR)) {
  fs.mkdirSync(TICKETS_DIR, { recursive: true });
}

/**
 * Generate a PDF ticket for a confirmed booking
 * @param {Object} booking - Populated booking document
 * @param {Object} schedule - Populated schedule with route and bus
 * @returns {String} - Path to the generated PDF
 */
async function generateTicket(booking, schedule) {
  const pnr = booking.pnr;
  const filePath = path.join(TICKETS_DIR, `${pnr}.pdf`);
  
  // Generate QR code as data URL (encodes booking ID for conductor scanning)
  const qrData = JSON.stringify({
    pnr: booking.pnr,
    bookingId: booking._id.toString(),
    seats: booking.seats,
    passengerName: booking.passengerName,
  });
  const qrImageDataUrl = await QRCode.toDataURL(qrData, { width: 150, margin: 1 });
  const qrBuffer = Buffer.from(qrImageDataUrl.split(',')[1], 'base64');
  
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({ size: 'A5', margin: 40 });
    const stream = fs.createWriteStream(filePath);
    
    doc.pipe(stream);
    
    // --- Header ---
    doc.fontSize(18).font('Helvetica-Bold')
       .fillColor('#0f766e')
       .text('DHAN-GAADI FLEET OS', { align: 'center' });
    doc.fontSize(10).font('Helvetica')
       .fillColor('#64748b')
       .text('Enterprise Intercity Transit & Ground Mobility', { align: 'center' });
    doc.moveDown(0.5);
    
    // Divider
    doc.moveTo(40, doc.y).lineTo(doc.page.width - 40, doc.y).stroke('#ccc');
    doc.moveDown(0.5);
    
    // --- E-TICKET label ---
    doc.fontSize(14).font('Helvetica-Bold')
       .fillColor('#0d9488')
       .text('VERIFIED DIGITAL BOARDING PASS', { align: 'center' });
    doc.moveDown(0.5);
    
    // --- Booking details ---
    const leftX = 40;
    const rightX = doc.page.width / 2 + 20;
    let y = doc.y;
    
    // Left column
    doc.fontSize(9).font('Helvetica-Bold').fillColor('#333');
    doc.text('PNR:', leftX, y);
    doc.font('Helvetica').text(booking.pnr, leftX + 80, y);
    
    y += 18;
    doc.font('Helvetica-Bold').text('Ref Code:', leftX, y);
    doc.font('Helvetica').text(booking.referenceCode, leftX + 80, y);
    
    y += 18;
    doc.font('Helvetica-Bold').text('Passenger:', leftX, y);
    doc.font('Helvetica').text(booking.passengerName, leftX + 80, y);
    
    y += 18;
    doc.font('Helvetica-Bold').text('Mobile:', leftX, y);
    doc.font('Helvetica').text(booking.passengerMobile, leftX + 80, y);
    
    y += 18;
    doc.font('Helvetica-Bold').text('Seats:', leftX, y);
    doc.font('Helvetica').text((booking.seats || []).join(', ') || 'N/A', leftX + 80, y);
    
    y += 18;
    doc.font('Helvetica-Bold').text('Fare Paid:', leftX, y);
    doc.font('Helvetica').text(`₹${booking.totalFare || 0}`, leftX + 80, y);

    if (booking.boardingPoint) {
      y += 18;
      doc.font('Helvetica-Bold').text('Boarding At:', leftX, y);
      doc.font('Helvetica').text(booking.boardingPoint, leftX + 80, y);
    }
    
    // Right column - Route details
    y = doc.y - (booking.boardingPoint ? 126 : 108); // Adjust according to left column height
    const route = schedule?.route || {};
    const bus = schedule?.bus || {};
    const routeTitle = (route.routeNumber ? `${route.routeNumber} — ` : '') + (route.name || 'Dhan-Gaadi Route');
    
    doc.font('Helvetica-Bold').text('Route:', rightX, y);
    doc.font('Helvetica').text(routeTitle, rightX + 70, y);
    
    y += 18;
    doc.font('Helvetica-Bold').text('From:', rightX, y);
    doc.font('Helvetica').text(route.source || 'Origin', rightX + 70, y);
    
    y += 18;
    doc.font('Helvetica-Bold').text('To:', rightX, y);
    doc.font('Helvetica').text(route.destination || 'Destination', rightX + 70, y);
    
    y += 18;
    doc.font('Helvetica-Bold').text('Departure:', rightX, y);
    doc.font('Helvetica').text(schedule?.departureTime || '--:--', rightX + 70, y);
    
    y += 18;
    doc.font('Helvetica-Bold').text('Bus:', rightX, y);
    const busText = (bus.busNumber || 'Fleet Bus') + (bus.type ? ` (${bus.type})` : '');
    doc.font('Helvetica').text(busText, rightX + 70, y);
    
    y += 18;
    const journeyDateStr = booking.journeyDate ? new Date(booking.journeyDate).toLocaleDateString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric'
    }) : '--';
    doc.font('Helvetica-Bold').text('Date:', rightX, y);
    doc.font('Helvetica').text(journeyDateStr, rightX + 70, y);
    
    doc.moveDown(4);
    
    // Divider
    doc.moveTo(40, doc.y).lineTo(doc.page.width - 40, doc.y).stroke('#ccc');
    doc.moveDown(0.5);
    
    // QR Code
    doc.image(qrBuffer, (doc.page.width - 120) / 2, doc.y, { width: 120 });
    doc.moveDown(8);
    
    doc.fontSize(7).font('Helvetica')
       .fillColor('#999')
       .text('Show this QR code to the conductor for verification', { align: 'center' });
    doc.moveDown(1);
    
    // Footer
    doc.fontSize(7).fillColor('#999')
       .text('This is a computer-generated ticket. No signature required.', { align: 'center' });
    doc.text(`Generated: ${new Date().toLocaleString('en-IN')}`, { align: 'center' });
    
    doc.on('error', reject);
    stream.on('finish', () => resolve(filePath));
    stream.on('error', reject);
    
    doc.end();
  });
}

module.exports = { generateTicket };
