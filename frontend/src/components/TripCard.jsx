import { useState } from 'react';
import { ChevronDown, ChevronUp, TrendingUp } from 'lucide-react';
import ModeIcon from './ModeIcon';
import PricePanel from './PricePanel';
import { fmtDuration, fmtTime, inr } from '../utils';

export default function TripCard({ trip, pax, lowest, onBook }) {
  const [open, setOpen] = useState(false);
  const soldOut = trip.seatsLeft < pax;
  const modeKey = trip.mode.toLowerCase();

  return (
    <article className={`ticket ${soldOut ? 'is-soldout' : ''}`}>
      <div className="ticket-body">
        <div className="ticket-main">
          <div className="ticket-operator">
            <span className={`mode-badge ${modeKey}`}>
              <ModeIcon mode={trip.mode} size={18} />
            </span>
            <div>
              <strong>{trip.operator}</strong>
              <small>{trip.code}</small>
            </div>
            <span className="tag">{trip.travelClass}</span>
            {lowest && <span className="tag tag-lowest">Lowest fare</span>}
          </div>

          <div className="ticket-times">
            <div className="ticket-stop">
              <b>{fmtTime(trip.departureTime)}</b>
              <small>{trip.origin} ({trip.originCode})</small>
            </div>
            <div className="ticket-line" aria-label={`Duration ${fmtDuration(trip.durationMinutes)}`}>
              <small>{fmtDuration(trip.durationMinutes)}</small>
              <span />
            </div>
            <div className="ticket-stop ticket-stop-end">
              <b>
                {fmtTime(trip.arrivalTime)}
                {trip.arrivalDayOffset > 0 && <sup>+{trip.arrivalDayOffset}</sup>}
              </b>
              <small>{trip.destination} ({trip.destinationCode})</small>
            </div>
          </div>
        </div>

        <div className="ticket-stub">
          <div className="ticket-price">
            <b>{inr(trip.price)}</b>
            <small>per person</small>
          </div>
          {!soldOut && trip.seatsLeft <= 12 && <span className="urgent">Only {trip.seatsLeft} left</span>}
          <button className="btn btn-primary" disabled={soldOut} onClick={onBook}>
            {soldOut ? 'Not enough seats' : pax > 1 ? `Book for ${inr(trip.price * pax)}` : 'Book'}
          </button>
          <button className="btn-link" onClick={() => setOpen(!open)} aria-expanded={open}>
            <TrendingUp size={16} aria-hidden="true" /> Price trend
            {open ? <ChevronUp size={16} aria-hidden="true" /> : <ChevronDown size={16} aria-hidden="true" />}
          </button>
        </div>
      </div>
      {open && <PricePanel kind="TRIP" id={trip.id} date={trip.travelDate} />}
    </article>
  );
}
