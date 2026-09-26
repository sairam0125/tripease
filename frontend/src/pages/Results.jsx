import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { Sparkles } from 'lucide-react';
import api, { errMsg } from '../api';
import SearchForm from '../components/SearchForm';
import TripCard from '../components/TripCard';
import { EmptyState, ErrorState, ListSkeleton } from '../components/States';
import { fmtDateLong, plural } from '../utils';

const SORTS = [
  { key: 'price', label: 'Lowest price' },
  { key: 'duration', label: 'Fastest' },
  { key: 'departure', label: 'Earliest departure' },
];

export default function Results() {
  const { mode } = useParams();
  const [sp] = useSearchParams();
  const nav = useNavigate();
  const from = sp.get('from');
  const to = sp.get('to');
  const date = sp.get('date');
  const pax = Number(sp.get('pax') || 1);

  const [items, setItems] = useState(null);
  const [error, setError] = useState('');
  const [sort, setSort] = useState('price');
  const [tip, setTip] = useState(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let live = true;
    setItems(null);
    setError('');
    setTip(null);
    api
      .get('/api/trips/search', { params: { mode: mode.toUpperCase(), from, to, date } })
      .then((r) => live && setItems(r.data))
      .catch((e) => live && setError(errMsg(e)));
    return () => {
      live = false;
    };
  }, [mode, from, to, date, attempt]);

  const sorted = useMemo(() => {
    if (!items) return [];
    const list = [...items];
    if (sort === 'price') list.sort((a, b) => a.price - b.price);
    else if (sort === 'duration') list.sort((a, b) => a.durationMinutes - b.durationMinutes);
    else list.sort((a, b) => a.departureTime.localeCompare(b.departureTime));
    return list;
  }, [items, sort]);

  const cheapest = useMemo(() => (items?.length ? [...items].sort((a, b) => a.price - b.price)[0] : null), [items]);

  // AI tip for the cheapest option on this route
  useEffect(() => {
    if (!cheapest) return;
    let live = true;
    api
      .get('/api/ai/insight', { params: { kind: 'TRIP', id: cheapest.id, date } })
      .then((r) => live && setTip(r.data))
      .catch(() => {});
    return () => {
      live = false;
    };
  }, [cheapest, date]);

  const book = (trip) => nav(`/checkout/trip/${trip.id}?date=${date}&qty=${pax}`);

  return (
    <div className="container page">
      <SearchForm compact initialTab={mode} initial={{ from, to, date, pax }} />

      <div className="results-head">
        <div>
          <h2>{from} to {to}</h2>
          <p>{date && fmtDateLong(date)}, {plural(pax, 'traveller')}</p>
        </div>
        <div className="chips" role="group" aria-label="Sort results">
          {SORTS.map((s) => (
            <button key={s.key} className={`chip ${sort === s.key ? 'active' : ''}`} onClick={() => setSort(s.key)} aria-pressed={sort === s.key}>
              {s.label}
            </button>
          ))}
        </div>
      </div>

      {tip && (
        <div className="tip" role="note">
          <Sparkles size={18} aria-hidden="true" />
          <p>
            <strong>Price tip for the lowest fare:</strong> {tip.insight}
          </p>
        </div>
      )}

      {error ? (
        <ErrorState message={error} onRetry={() => setAttempt((n) => n + 1)} />
      ) : !items ? (
        <ListSkeleton />
      ) : items.length === 0 ? (
        <EmptyState title={`No ${mode} services on this route`}>
          Try another mode or a nearby city. Trains and buses run only on shorter routes.
        </EmptyState>
      ) : (
        <div className="stack">
          {sorted.map((t) => (
            <TripCard key={t.id} trip={t} pax={pax} lowest={cheapest && t.id === cheapest.id && items.length > 1} onBook={() => book(t)} />
          ))}
        </div>
      )}
    </div>
  );
}
