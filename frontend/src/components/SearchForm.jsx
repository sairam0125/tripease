import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plane, Train, Bus, Hotel, ArrowRightLeft, Search } from 'lucide-react';
import { useCities } from '../hooks/useCities';
import { addDays, today } from '../utils';

const TABS = [
  { key: 'flight', label: 'Flights', verb: 'Search flights', Icon: Plane },
  { key: 'train', label: 'Trains', verb: 'Search trains', Icon: Train },
  { key: 'bus', label: 'Buses', verb: 'Search buses', Icon: Bus },
  { key: 'hotel', label: 'Stays', verb: 'Search stays', Icon: Hotel },
];

function Field({ label, children }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
    </label>
  );
}

export default function SearchForm({ initialTab = 'flight', initial = {}, compact = false }) {
  const nav = useNavigate();
  const cities = useCities();

  const [tab, setTab] = useState(initialTab);
  const [from, setFrom] = useState(initial.from || 'Hyderabad');
  const [to, setTo] = useState(initial.to || 'Bengaluru');
  const [date, setDate] = useState(initial.date || addDays(today(), 7));
  const [pax, setPax] = useState(initial.pax || 1);
  const [city, setCity] = useState(initial.city || 'Goa');
  const [checkIn, setCheckIn] = useState(initial.checkIn || addDays(today(), 7));
  const [checkOut, setCheckOut] = useState(initial.checkOut || addDays(today(), 9));
  const [rooms, setRooms] = useState(initial.rooms || 1);
  const [error, setError] = useState('');

  useEffect(() => {
    setTab(initialTab);
  }, [initialTab]);

  const onCheckIn = (value) => {
    setCheckIn(value);
    if (checkOut <= value) setCheckOut(addDays(value, 1));
  };

  const swap = () => {
    setFrom(to);
    setTo(from);
  };

  const submit = (e) => {
    e.preventDefault();
    setError('');
    if (tab === 'hotel') {
      if (!checkIn || !checkOut || checkOut <= checkIn) return setError('Check-out must be after check-in.');
      nav(`/hotels?city=${encodeURIComponent(city)}&checkIn=${checkIn}&checkOut=${checkOut}&rooms=${rooms}`);
    } else {
      if (from === to) return setError('Choose two different cities.');
      if (!date) return setError('Pick a travel date.');
      nav(`/search/${tab}?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&date=${date}&pax=${pax}`);
    }
  };

  const active = TABS.find((t) => t.key === tab) || TABS[0];

  return (
    <form className={`search-card ${compact ? 'compact' : ''}`} onSubmit={submit}>
      <div className="tabs" role="tablist" aria-label="What are you booking?">
        {TABS.map(({ key, label, Icon }) => (
          <button
            key={key}
            type="button"
            role="tab"
            aria-selected={tab === key}
            className={`tab ${tab === key ? 'active' : ''}`}
            onClick={() => setTab(key)}
          >
            <Icon size={18} aria-hidden="true" />
            {label}
          </button>
        ))}
      </div>

      {tab === 'hotel' ? (
        <div className="search-grid stay-grid">
          <Field label="City">
            <select value={city} onChange={(e) => setCity(e.target.value)}>
              {cities.map((c) => <option key={c}>{c}</option>)}
            </select>
          </Field>
          <Field label="Check-in">
            <input type="date" min={today()} value={checkIn} onChange={(e) => onCheckIn(e.target.value)} required />
          </Field>
          <Field label="Check-out">
            <input type="date" min={addDays(checkIn, 1)} value={checkOut} onChange={(e) => setCheckOut(e.target.value)} required />
          </Field>
          <Field label="Rooms">
            <select value={rooms} onChange={(e) => setRooms(Number(e.target.value))}>
              {[1, 2, 3, 4, 5].map((n) => <option key={n} value={n}>{n}</option>)}
            </select>
          </Field>
          <button className="btn btn-primary search-submit" type="submit">
            <Search size={18} aria-hidden="true" /> {active.verb}
          </button>
        </div>
      ) : (
        <div className="search-grid route-grid">
          <Field label="From">
            <select value={from} onChange={(e) => setFrom(e.target.value)}>
              {cities.map((c) => <option key={c}>{c}</option>)}
            </select>
          </Field>
          <button type="button" className="swap" onClick={swap} aria-label="Swap origin and destination">
            <ArrowRightLeft size={18} aria-hidden="true" />
          </button>
          <Field label="To">
            <select value={to} onChange={(e) => setTo(e.target.value)}>
              {cities.map((c) => <option key={c}>{c}</option>)}
            </select>
          </Field>
          <Field label="Date">
            <input type="date" min={today()} value={date} onChange={(e) => setDate(e.target.value)} required />
          </Field>
          <Field label="Travellers">
            <select value={pax} onChange={(e) => setPax(Number(e.target.value))}>
              {[1, 2, 3, 4, 5, 6, 7, 8, 9].map((n) => <option key={n} value={n}>{n}</option>)}
            </select>
          </Field>
          <button className="btn btn-primary search-submit" type="submit">
            <Search size={18} aria-hidden="true" /> {active.verb}
          </button>
        </div>
      )}
      {error && <p className="form-error" role="alert">{error}</p>}
    </form>
  );
}
