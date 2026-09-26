import { useState } from 'react';
import { ChevronDown, ChevronUp, Star, TrendingUp } from 'lucide-react';
import PricePanel from './PricePanel';
import { inr, plural } from '../utils';

export default function HotelCard({ hotel, rooms, onBook }) {
  const [open, setOpen] = useState(false);
  const soldOut = hotel.roomsLeft < rooms;
//  const hue = (hotel.id * 47) % 360;

  return (
    <article className={`hotel ${soldOut ? 'is-soldout' : ''}`}>
      <div className="hotel-row">
        {/* <div className="hotel-art" style={{ background: `linear-gradient(140deg, hsl(${hue} 55% 32%), hsl(${(hue + 40) % 360} 60% 52%))` }}>
          <span className="hotel-kind">{hotel.type === 'RESORT' ? 'Resort' : 'Hotel'}</span>
          <span className="hotel-stars" aria-label={`${hotel.stars} star`}>
            {Array.from({ length: hotel.stars }).map((_, i) => <Star key={i} size={13} fill="currentColor" />)}
          </span>
        </div> */}
  <div className="hotel-art">
  <img
    src={`/hotels/hotel-${hotel.id}.jpg`}
    alt={hotel.name}
    className="hotel-image"
    onError={(e) => {
      e.currentTarget.src = '/hotels/default-hotel.jpg';
    }}
  />

  <span className="hotel-kind">
    {hotel.type === 'RESORT' ? 'Resort' : 'Hotel'}
  </span>

  <span
    className="hotel-stars"
    aria-label={`${hotel.stars} star`}
  >
    {Array.from({ length: hotel.stars }).map((_, i) => (
      <Star
        key={i}
        size={13}
        fill="currentColor"
      />
    ))}
  </span>
</div>

        <div className="hotel-info">
          <div className="hotel-title">
            <h3>{hotel.name}</h3>
            <span className="rating" title="Guest rating">{hotel.rating.toFixed(1)}</span>
          </div>
          <p className="hotel-desc">{hotel.description}</p>
          <ul className="tags">
            {hotel.amenities.map((a) => <li key={a} className="tag">{a}</li>)}
          </ul>
        </div>

        <div className="ticket-stub hotel-stub">
          <div className="ticket-price">
            <b>{inr(hotel.pricePerNight)}</b>
            <small>per night</small>
          </div>
          <small className="hotel-total">
            {inr(hotel.totalPrice * rooms)} for {plural(hotel.nights, 'night')}
            {rooms > 1 ? `, ${plural(rooms, 'room')}` : ''}
          </small>
          {!soldOut && hotel.roomsLeft <= 8 && <span className="urgent">Only {hotel.roomsLeft} rooms left</span>}
          <button className="btn btn-primary" disabled={soldOut} onClick={onBook}>
            {soldOut ? 'Not enough rooms' : 'Book'}
          </button>
          <button className="btn-link" onClick={() => setOpen(!open)} aria-expanded={open}>
            <TrendingUp size={16} aria-hidden="true" /> Price trend
            {open ? <ChevronUp size={16} aria-hidden="true" /> : <ChevronDown size={16} aria-hidden="true" />}
          </button>
        </div>
      </div>
      {open && <PricePanel kind="HOTEL" id={hotel.id} date={hotel.checkIn} />}
    </article>
  );
}
