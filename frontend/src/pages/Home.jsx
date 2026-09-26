import { Link, useSearchParams } from 'react-router-dom';
import { BarChart3, Bot, Layers, ShieldCheck } from 'lucide-react';
import SearchForm from '../components/SearchForm';
import ModeIcon from '../components/ModeIcon';
import { addDays, today } from '../utils';

const BOARD = [
  { mode: 'FLIGHT', label: 'Flight', from: 'Hyderabad', to: 'Goa', path: 'flight', runs: 'Daily' },
  { mode: 'TRAIN', label: 'Train', from: 'Chennai', to: 'Bengaluru', path: 'train', runs: 'Daily' },
  { mode: 'BUS', label: 'Bus', from: 'Mumbai', to: 'Pune', path: 'bus', runs: 'Daily' },
  { mode: 'FLIGHT', label: 'Flight', from: 'Delhi', to: 'Kolkata', path: 'flight', runs: 'Daily' },
  { mode: 'HOTEL', label: 'Resorts', from: null, to: 'Goa', path: 'hotel', runs: 'Rooms open' },
];

const FEATURES = [
  { Icon: Layers, title: 'Every way to travel', text: 'Flights, trains, buses, hotels and resorts in one search and one booking history.' },
  { Icon: BarChart3, title: 'See the price before it moves', text: 'A price graph for every fare shows the cheapest nearby dates.' },
  { Icon: Bot, title: 'A planner you can talk to', text: 'Tell the assistant your route and budget. It answers with real options you can open in one tap.' },
  { Icon: ShieldCheck, title: 'Safe sign-in', text: 'Accounts use hashed passwords and signed tokens. Fares are re-checked on the server when you book.' },
];

export default function Home() {
  const [sp] = useSearchParams();
  const mode = ['flight', 'train', 'bus', 'hotel'].includes(sp.get('mode')) ? sp.get('mode') : 'flight';
  const soon = addDays(today(), 7);

  const boardLink = (r) =>
    r.mode === 'HOTEL'
      ? `/hotels?city=${r.to}&checkIn=${soon}&checkOut=${addDays(soon, 2)}&rooms=1`
      : `/search/${r.path}?from=${r.from}&to=${r.to}&date=${soon}&pax=1`;

  return (
    <>
      <section className="hero">
        <div className="container hero-grid">
          <div className="hero-copy">
            <h1>One counter for every way to go.</h1>
            <p>
              Compare flights, trains, buses and stays side by side, check the price trend for your dates, and book
              the whole trip in one place.
            </p>
          </div>

          <div className="board" role="list" aria-label="Popular routes">
            <div className="board-head">
              <span>Popular routes</span>
              <span>Runs</span>
            </div>
            {BOARD.map((r, i) => (
              <Link key={i} to={boardLink(r)} className="board-row" role="listitem" style={{ animationDelay: `${i * 110}ms` }}>
                <span className={`board-mode ${r.mode.toLowerCase()}`}>
                  <ModeIcon mode={r.mode} size={16} />
                  {r.label}
                </span>
                <span className="board-route">{r.from ? `${r.from} to ${r.to}` : r.to}</span>
                <span className="board-runs">{r.runs}</span>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <div className="container search-wrap">
        <SearchForm initialTab={mode} />
      </div>

      <section className="container features" aria-label="Why TripEase">
        {FEATURES.map(({ Icon, title, text }) => (
          <div key={title} className="feature">
            <Icon size={22} aria-hidden="true" />
            <h3>{title}</h3>
            <p>{text}</p>
          </div>
        ))}
      </section>
    </>
  );
}
