import { useEffect, useState } from 'react';
import { Area, AreaChart, CartesianGrid, ReferenceDot, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Sparkles } from 'lucide-react';
import api, { errMsg } from '../api';
import { fmtDate, inr } from '../utils';

const shortDate = (iso) => {
  const [, m, d] = iso.split('-');
  return `${Number(d)}/${Number(m)}`;
};

function ChartTip({ active, payload }) {
  if (!active || !payload?.length) return null;
  const p = payload[0].payload;
  return (
    <div className="chart-tip">
      <b>{inr(p.price)}</b>
      <span>{fmtDate(p.date)}</span>
    </div>
  );
}

/**
 * Price graph + AI insight for one trip or hotel.
 * kind: 'TRIP' | 'HOTEL'.  date: travel date / check-in (YYYY-MM-DD)
 */
export default function PricePanel({ kind, id, date }) {
  const [series, setSeries] = useState(null);
  const [insight, setInsight] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let live = true;
    setSeries(null);
    setInsight(null);
    setError('');
    const base = kind === 'TRIP' ? 'trips' : 'hotels';
    api
      .get(`/api/prices/${base}/${id}`, { params: { date } })
      .then((r) => live && setSeries(r.data))
      .catch((e) => live && setError(errMsg(e)));
    api
      .get('/api/ai/insight', { params: { kind, id, date } })
      .then((r) => live && setInsight(r.data))
      .catch(() => {});
    return () => {
      live = false;
    };
  }, [kind, id, date]);

  if (error) return <div className="price-panel"><p className="form-error">{error}</p></div>;
  if (!series) {
    return (
      <div className="price-panel" aria-busy="true">
        <div className="skeleton skeleton-chart" />
      </div>
    );
  }

  const unit = kind === 'TRIP' ? 'per person' : 'per night';

  return (
    <div className="price-panel">
      <div className="price-panel-head">
        <h4>Price by date, {unit}</h4>
        <span className="legend">
          <i className="dot dot-you" /> Your date
          <i className="dot dot-low" /> Lowest
        </span>
      </div>

      <div className="chart-wrap">
        <ResponsiveContainer width="100%" height={210}>
          <AreaChart data={series.points} margin={{ top: 12, right: 12, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id="priceFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#2145F5" stopOpacity={0.28} />
                <stop offset="100%" stopColor="#2145F5" stopOpacity={0.02} />
              </linearGradient>
            </defs>
            <CartesianGrid stroke="#E3E8F5" vertical={false} />
            <XAxis dataKey="date" tickFormatter={shortDate} tick={{ fontSize: 12, fill: '#6C7799' }} tickLine={false} axisLine={false} interval="preserveStartEnd" minTickGap={28} />
            <YAxis
              width={54}
              tick={{ fontSize: 12, fill: '#6C7799' }}
              tickLine={false}
              axisLine={false}
              tickFormatter={(v) => (v >= 1000 ? `₹${(v / 1000).toFixed(1)}k` : `₹${v}`)}
              domain={[(min) => Math.floor((min * 0.92) / 100) * 100, (max) => Math.ceil((max * 1.04) / 100) * 100]}
            />
            <Tooltip content={<ChartTip />} cursor={{ stroke: '#2145F5', strokeDasharray: '3 3' }} />
            <Area type="monotone" dataKey="price" stroke="#2145F5" strokeWidth={2.5} fill="url(#priceFill)" />
            <ReferenceDot x={series.cheapestDate} y={series.min} r={5} fill="#0E8F73" stroke="#fff" strokeWidth={2} ifOverflow="visible" />
            <ReferenceDot x={series.selectedDate} y={series.selectedPrice} r={7} fill="#FFB627" stroke="#0E1A40" strokeWidth={2} ifOverflow="visible" />
          </AreaChart>
        </ResponsiveContainer>
      </div>

      <dl className="stats">
        <div><dt>Your date</dt><dd>{inr(series.selectedPrice)}</dd></div>
        <div><dt>Lowest ({fmtDate(series.cheapestDate)})</dt><dd className="good">{inr(series.min)}</dd></div>
        <div><dt>Average</dt><dd>{inr(series.average)}</dd></div>
        <div><dt>Highest</dt><dd>{inr(series.max)}</dd></div>
      </dl>

      <div className="insight">
        <Sparkles size={18} aria-hidden="true" />
        {insight ? (
          <div>
            <p>{insight.insight}</p>
            <small>
              {insight.source === 'rule-based' ? 'Calculated from fare trends' : `Written by ${insight.source}`}
            </small>
          </div>
        ) : (
          <div className="skeleton skeleton-line" aria-label="Loading insight" />
        )}
      </div>
    </div>
  );
}
