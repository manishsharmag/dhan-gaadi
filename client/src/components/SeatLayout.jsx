'use client';
import { useState, useMemo } from 'react';
import styles from './SeatLayout.module.css';

/**
 * Visual 2+2 bus seat layout with aisle
 * 11 rows × 4 seats (2-aisle-2) + 1 back row of 5 = 49 total positions, 45 usable seats
 * 
 * Layout:
 * [Driver]  [Door]
 * [1 ] [2 ] | [3 ] [4 ]
 * [5 ] [6 ] | [7 ] [8 ]
 * ...         ...
 * [41] [42] [43] [44] [45]  ← back row of 5
 */
export default function SeatLayout({ totalSeats = 45, unavailableSeats = [], selectedSeats = [], onSeatClick, disabled = false }) {
  // Build seat map from unavailable seats data
  const seatStatusMap = useMemo(() => {
    const map = {};
    unavailableSeats.forEach(s => {
      map[s.seatNumber] = s.status; // 'booked' or 'locked'
    });
    return map;
  }, [unavailableSeats]);

  const getSeatStatus = (seatNum) => {
    if (selectedSeats.includes(seatNum)) return 'selected';
    if (seatStatusMap[seatNum] === 'booked') return 'booked';
    if (seatStatusMap[seatNum] === 'locked') return 'locked';
    return 'available';
  };

  const handleClick = (seatNum) => {
    if (disabled) return;
    const status = getSeatStatus(seatNum);
    if (status === 'booked' || status === 'locked') return;
    onSeatClick?.(seatNum);
  };

  // Generate rows dynamically based on totalSeats (2+2 layout + 5 in the back row)
  const { regularRows, backRow } = useMemo(() => {
    const totalRegularSeats = Math.max(0, totalSeats - 5);
    const numRows = Math.ceil(totalRegularSeats / 4);

    const rows = [];
    let seatNum = 1;
    for (let r = 0; r < numRows; r++) {
      const rowSeats = [];
      for (let col = 0; col < 4; col++) {
        if (seatNum <= totalRegularSeats) {
          rowSeats.push(seatNum);
          seatNum++;
        }
      }
      if (rowSeats.length > 0) rows.push(rowSeats);
    }

    const back = [];
    for (let i = totalRegularSeats + 1; i <= totalSeats; i++) {
      if (i > 0) back.push(i);
    }

    return { regularRows: rows, backRow: back };
  }, [totalSeats]);

  return (
    <div className={styles.busContainer}>
      {/* Bus shell */}
      <div className={styles.busShell}>
        {/* Front of bus — Indian Right-Hand Drive (RHD): Passenger Door on Left, Driver on Right */}
        <div className={styles.busFront}>
          <div className={styles.doorArea} title="Passenger Boarding Gate (Front Left)">
            <div className={styles.doorIcon}>🚪</div>
            <span className={styles.doorLabel}>Entry Door</span>
          </div>
          <div className={styles.driverArea} title="Driver Cabin (Front Right - RHD)">
            <span className={styles.driverLabel}>Driver (RHD)</span>
            <div className={styles.steeringWheel}>🛞</div>
          </div>
        </div>

        {/* Column indicators: Window & Aisle */}
        <div className={styles.columnGuide}>
          <div className={styles.guidePair}>
            <span title="Window Seat">🪟 Win</span>
            <span title="Aisle Seat">🚶 Aisle</span>
          </div>
          <div className={styles.guideAisle}>Aisle</div>
          <div className={styles.guidePair}>
            <span title="Aisle Seat">🚶 Aisle</span>
            <span title="Window Seat (Driver Side)">🪟 Win</span>
          </div>
        </div>

        {/* Seat grid */}
        <div className={styles.seatGrid}>
          {regularRows.map((row) => (
            <div key={`row-${row[0]}`} className={styles.seatRow}>
              {/* Left pair (Door side) */}
              <div className={styles.seatPair}>
                {row.slice(0, 2).map(num => (
                  <button
                    key={num}
                    className={`${styles.seat} ${styles[getSeatStatus(num)]}`}
                    onClick={() => handleClick(num)}
                    disabled={disabled || getSeatStatus(num) === 'booked' || getSeatStatus(num) === 'locked'}
                    title={`Seat ${num} — ${getSeatStatus(num)} (${row.indexOf(num) === 0 ? 'Window' : 'Aisle'})`}
                    aria-label={`Seat ${num}, ${getSeatStatus(num)}`}
                  >
                    <span className={styles.seatNumber}>{num}</span>
                  </button>
                ))}
              </div>
              
              {/* Aisle */}
              <div className={styles.aisle} />
              
              {/* Right pair (Driver side) */}
              <div className={styles.seatPair}>
                {row.slice(2, 4).map(num => (
                  <button
                    key={num}
                    className={`${styles.seat} ${styles[getSeatStatus(num)]}`}
                    onClick={() => handleClick(num)}
                    disabled={disabled || getSeatStatus(num) === 'booked' || getSeatStatus(num) === 'locked'}
                    title={`Seat ${num} — ${getSeatStatus(num)} (${row.indexOf(num) === 3 ? 'Window (Driver side)' : 'Aisle'})`}
                    aria-label={`Seat ${num}, ${getSeatStatus(num)}`}
                  >
                    <span className={styles.seatNumber}>{num}</span>
                  </button>
                ))}
              </div>
            </div>
          ))}

          {/* Back row — 5 seats across */}
          <div className={`${styles.seatRow} ${styles.backRow}`}>
            {backRow.map(num => (
              <button
                key={num}
                className={`${styles.seat} ${styles[getSeatStatus(num)]}`}
                onClick={() => handleClick(num)}
                disabled={disabled || getSeatStatus(num) === 'booked' || getSeatStatus(num) === 'locked'}
                title={`Seat ${num} — ${getSeatStatus(num)}`}
                aria-label={`Seat ${num}, ${getSeatStatus(num)}`}
              >
                <span className={styles.seatNumber}>{num}</span>
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Legend */}
      <div className={styles.legend}>
        <div className={styles.legendItem}>
          <div className={`${styles.legendDot} ${styles.available}`} />
          <span>Available</span>
        </div>
        <div className={styles.legendItem}>
          <div className={`${styles.legendDot} ${styles.selected}`} />
          <span>Selected</span>
        </div>
        <div className={styles.legendItem}>
          <div className={`${styles.legendDot} ${styles.booked}`} />
          <span>Booked</span>
        </div>
        <div className={styles.legendItem}>
          <div className={`${styles.legendDot} ${styles.locked}`} />
          <span>Locked</span>
        </div>
      </div>
    </div>
  );
}
