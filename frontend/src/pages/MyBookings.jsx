import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { CheckCircle2 } from 'lucide-react';
import api, { errMsg } from '../api';
import ModeIcon from '../components/ModeIcon';
import { EmptyState, ErrorState, ListSkeleton } from '../components/States';
import { fmtDate, fmtTime, inr, plural } from '../utils';

export default function MyBookings() {
  const location = useLocation();
  const justBooked = location.state?.justBooked;
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let live = true;
    setError('');
    api
      .get('/api/bookings')
      .then((r) => live && setItems(r.data))
      .catch((e) => live && setError(errMsg(e)));
    return () => {
      live = false;
    };
  }, [attempt]);

  const cancel = async (b) => {
    if (!window.confirm(`Cancel booking ${b.reference}? This cannot be undone.`)) return;
    setActionError('');
    try {
      const res = await api.post(`/api/bookings/${b.id}/cancel`);
      setItems((list) => list.map((x) => (x.id === b.id ? res.data : x)));
    } catch (e) {
      setActionError(errMsg(e));
    }
  };

  return (
    <div className="container page">
      <h2 className="page-title">My trips</h2>

      {justBooked && (
        <div className="success" role="status">
          <CheckCircle2 size={22} aria-hidden="true" />
          <div>
            <strong>Booking confirmed</strong>
            <p>Your reference is <b>{justBooked}</b>. Have a great trip!</p>
          </div>
        </div>
      )}
      {actionError && <p className="form-error" role="alert">{actionError}</p>}

      {error ? (
        <ErrorState message={error} onRetry={() => setAttempt((n) => n + 1)} />
      ) : !items ? (
        <ListSkeleton />
      ) : items.length === 0 ? (
        <EmptyState title="No bookings yet">
          <Link to="/" className="btn-link">Search flights, trains, buses or stays</Link> to plan your first trip.
        </EmptyState>
      ) : (
        <div className="stack">
          {items.map((b) => {
            const cancelled = b.status === 'CANCELLED';
            const isHotel = b.type === 'HOTEL';
            return (
              <article key={b.id} className={`booking ${cancelled ? 'is-cancelled' : ''}`}>
                <span className={`mode-badge ${b.type.toLowerCase()}`}><ModeIcon mode={b.type} /></span>
                <div className="booking-main">
                  <h3>{b.title}</h3>
                  <p>{b.subtitle}</p>
                  <p className="booking-when">
                    {isHotel
                      ? `${fmtDate(b.travelDate)} to ${fmtDate(b.checkOutDate)}, ${plural(b.quantity, 'room')}`
                      : `${fmtDate(b.travelDate)}, ${fmtTime(b.departureTime)} to ${fmtTime(b.arrivalTime)}, ${plural(b.quantity, 'seat')}`}
                  </p>
                  <p className="booking-who">{b.travellerNames}</p>
                </div>
                <div className="booking-side">
                  <span className={`status ${cancelled ? 'status-cancelled' : 'status-ok'}`}>{cancelled ? 'Cancelled' : 'Confirmed'}</span>
                  <b>{inr(b.totalAmount)}</b>
                  <small>Ref {b.reference}</small>
                  {!cancelled && <button className="btn-link danger" onClick={() => cancel(b)}>Cancel booking</button>}
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
