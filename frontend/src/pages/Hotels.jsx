import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Sparkles } from 'lucide-react';
import api, { errMsg } from '../api';
import SearchForm from '../components/SearchForm';
import HotelCard from '../components/HotelCard';
import { EmptyState, ErrorState, ListSkeleton } from '../components/States';
import { fmtDate, plural } from '../utils';

export default function Hotels() {
  const [sp] = useSearchParams();
  const nav = useNavigate();
  const city = sp.get('city');
  const checkIn = sp.get('checkIn');
  const checkOut = sp.get('checkOut');
  const rooms = Number(sp.get('rooms') || 1);

  const [items, setItems] = useState(null);
  const [error, setError] = useState('');
  const [type, setType] = useState('ALL');
  const [sort, setSort] = useState('price');
  const [tip, setTip] = useState(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let live = true;
    setItems(null);
    setError('');
    setTip(null);
    api
      .get('/api/hotels/search', { params: { city, checkIn, checkOut } })
      .then((r) => live && setItems(r.data))
      .catch((e) => live && setError(errMsg(e)));
    return () => {
      live = false;
    };
  }, [city, checkIn, checkOut, attempt]);

  const shown = useMemo(() => {
    if (!items) return [];
    const list = items.filter((h) => type === 'ALL' || h.type === type);
    list.sort(sort === 'price' ? (a, b) => a.pricePerNight - b.pricePerNight : (a, b) => b.rating - a.rating);
    return list;
  }, [items, type, sort]);

  const cheapest = useMemo(() => (items?.length ? [...items].sort((a, b) => a.pricePerNight - b.pricePerNight)[0] : null), [items]);

  useEffect(() => {
    if (!cheapest) return;
    let live = true;
    api
      .get('/api/ai/insight', { params: { kind: 'HOTEL', id: cheapest.id, date: checkIn } })
      .then((r) => live && setTip(r.data))
      .catch(() => {});
    return () => {
      live = false;
    };
  }, [cheapest, checkIn]);

  const book = (h) => nav(`/checkout/hotel/${h.id}?checkIn=${checkIn}&checkOut=${checkOut}&qty=${rooms}`);

  return (
    <div className="container page">
      <SearchForm compact initialTab="hotel" initial={{ city, checkIn, checkOut, rooms }} />

      <div className="results-head">
        <div>
          <h2>Stays in {city}</h2>
          <p>
            {checkIn && fmtDate(checkIn)} to {checkOut && fmtDate(checkOut)}, {plural(rooms, 'room')}
          </p>
        </div>
        <div className="chips">
          {[['ALL', 'All stays'], ['HOTEL', 'Hotels'], ['RESORT', 'Resorts']].map(([k, label]) => (
            <button key={k} className={`chip ${type === k ? 'active' : ''}`} onClick={() => setType(k)} aria-pressed={type === k}>{label}</button>
          ))}
          <span className="chips-sep" aria-hidden="true" />
          {[['price', 'Lowest price'], ['rating', 'Best rated']].map(([k, label]) => (
            <button key={k} className={`chip ${sort === k ? 'active' : ''}`} onClick={() => setSort(k)} aria-pressed={sort === k}>{label}</button>
          ))}
        </div>
      </div>

      {tip && (
        <div className="tip" role="note">
          <Sparkles size={18} aria-hidden="true" />
          <p><strong>Price tip for the lowest rate:</strong> {tip.insight}</p>
        </div>
      )}

      {error ? (
        <ErrorState message={error} onRetry={() => setAttempt((n) => n + 1)} />
      ) : !items ? (
        <ListSkeleton />
      ) : shown.length === 0 ? (
        <EmptyState title="No stays match">Try All stays, or change your dates or city.</EmptyState>
      ) : (
        <div className="stack">
          {shown.map((h) => <HotelCard key={h.id} hotel={h} rooms={rooms} onBook={() => book(h)} />)}
        </div>
      )}
    </div>
  );
}
