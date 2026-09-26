import { useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { CreditCard, Landmark, Smartphone } from 'lucide-react';
import api, { errMsg } from '../api';
import { useAuth } from '../context/AuthContext';
import ModeIcon from '../components/ModeIcon';
import { ErrorState, ListSkeleton } from '../components/States';
import { fmtDate, fmtDateLong, fmtDuration, fmtTime, inr, plural } from '../utils';

const PAYMENTS = [
  { key: 'UPI', label: 'UPI', Icon: Smartphone },
  { key: 'CARD', label: 'Card', Icon: CreditCard },
  { key: 'NETBANKING', label: 'Net banking', Icon: Landmark },
];

export default function Checkout() {
  const { kind, id } = useParams(); // 'trip' | 'hotel'
  const [sp] = useSearchParams();
  const nav = useNavigate();
  const { user } = useAuth();

  const qty = Math.max(1, Number(sp.get('qty') || 1));
  const date = sp.get('date');
  const checkIn = sp.get('checkIn');
  const checkOut = sp.get('checkOut');
  const isTrip = kind === 'trip';

  const [item, setItem] = useState(null);
  const [error, setError] = useState('');
  const [names, setNames] = useState(Array.from({ length: isTrip ? qty : 1 }, (_, i) => (i === 0 ? user?.name || '' : '')));
  const [payment, setPayment] = useState('UPI');
  const [busy, setBusy] = useState(false);
  const [formError, setFormError] = useState('');

  useEffect(() => {
    let live = true;
    const req = isTrip
      ? api.get(`/api/trips/${id}`, { params: { date } })
      : api.get(`/api/hotels/${id}`, { params: { checkIn, checkOut } });
    req.then((r) => live && setItem(r.data)).catch((e) => live && setError(errMsg(e)));
    return () => {
      live = false;
    };
  }, [isTrip, id, date, checkIn, checkOut]);

  if (error) return <div className="container page"><ErrorState message={error} onRetry={() => nav(-1)} /></div>;
  if (!item) return <div className="container page"><ListSkeleton rows={2} /></div>;

  const total = isTrip ? item.price * qty : item.totalPrice * qty;

  const pay = async (e) => {
    e.preventDefault();
    setFormError('');
    if (names.some((n) => !n.trim())) return setFormError(isTrip ? 'Enter the name of every traveller.' : 'Enter the guest name.');
    setBusy(true);
    try {
      const res = await api.post('/api/bookings', {
        type: isTrip ? item.mode : 'HOTEL',
        tripId: isTrip ? item.id : null,
        hotelId: isTrip ? null : item.id,
        travelDate: isTrip ? date : checkIn,
        checkOutDate: isTrip ? null : checkOut,
        quantity: qty,
        travellerNames: names.map((n) => n.trim()).join(', '),
        paymentMethod: payment,
      });
      nav('/bookings', { state: { justBooked: res.data.reference } });
    } catch (err) {
      setFormError(errMsg(err));
      setBusy(false);
    }
  };

  return (
    <div className="container page">
      <h2 className="page-title">Review and pay</h2>
      <div className="checkout">
        <form className="panel" onSubmit={pay}>
          <h3>{isTrip ? 'Traveller details' : 'Guest details'}</h3>
          {names.map((n, i) => (
            <label className="field field-wide" key={i}>
              <span>{isTrip ? `Traveller ${i + 1} full name` : 'Primary guest full name'}</span>
              <input
                value={n}
                maxLength={80}
                onChange={(e) => setNames(names.map((x, j) => (j === i ? e.target.value : x)))}
                placeholder="As on government ID"
                required
              />
            </label>
          ))}

          <h3>Payment method</h3>
          <div className="pay-options" role="radiogroup" aria-label="Payment method">
            {PAYMENTS.map(({ key, label, Icon }) => (
              <label key={key} className={`pay-opt ${payment === key ? 'active' : ''}`}>
                <input type="radio" name="pay" value={key} checked={payment === key} onChange={() => setPayment(key)} />
                <Icon size={18} aria-hidden="true" /> {label}
              </label>
            ))}
          </div>
          <p className="hint">Demo mode: no real payment is taken and no card details are collected.</p>

          {formError && <p className="form-error" role="alert">{formError}</p>}
          <button className="btn btn-primary btn-lg" disabled={busy} type="submit">
            {busy ? 'Confirming...' : `Pay ${inr(total)} and confirm`}
          </button>
        </form>

        <aside className="panel summary" aria-label="Booking summary">
          {isTrip ? (
            <>
              <div className="summary-title">
                <span className={`mode-badge ${item.mode.toLowerCase()}`}><ModeIcon mode={item.mode} /></span>
                <div>
                  <strong>{item.operator}</strong>
                  <small>{item.code}, {item.travelClass}</small>
                </div>
              </div>
              <div className="summary-route">
                <div><b>{fmtTime(item.departureTime)}</b><small>{item.origin}</small></div>
                <span>{fmtDuration(item.durationMinutes)}</span>
                <div><b>{fmtTime(item.arrivalTime)}{item.arrivalDayOffset > 0 && <sup>+{item.arrivalDayOffset}</sup>}</b><small>{item.destination}</small></div>
              </div>
              <p className="summary-date">{fmtDateLong(date)}</p>
              <div className="summary-line"><span>{inr(item.price)} x {plural(qty, 'traveller')}</span><b>{inr(total)}</b></div>
            </>
          ) : (
            <>
              <div className="summary-title">
                <span className="mode-badge hotel"><ModeIcon mode="HOTEL" /></span>
                <div>
                  <strong>{item.name}</strong>
                  <small>{item.city}, {item.stars}-star {item.type === 'RESORT' ? 'resort' : 'hotel'}</small>
                </div>
              </div>
              <p className="summary-date">{fmtDate(checkIn)} to {fmtDate(checkOut)} ({plural(item.nights, 'night')})</p>
              <div className="summary-line"><span>{inr(item.totalPrice)} x {plural(qty, 'room')}</span><b>{inr(total)}</b></div>
            </>
          )}
          <div className="summary-total"><span>Total, all taxes included</span><b>{inr(total)}</b></div>
          <p className="hint">The final amount is confirmed by the server when you pay.</p>
        </aside>
      </div>
    </div>
  );
}
