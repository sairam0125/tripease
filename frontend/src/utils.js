export const inr = (n) => '₹' + Math.round(Number(n)).toLocaleString('en-IN');

export const fmtDuration = (min) => `${Math.floor(min / 60)}h ${String(min % 60).padStart(2, '0')}m`;

export const fmtTime = (t) => (t ? String(t).slice(0, 5) : '');

const pad = (n) => String(n).padStart(2, '0');
export const toISO = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
export const today = () => toISO(new Date());

const parse = (iso) => {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
};
export const addDays = (iso, n) => {
  const d = parse(iso);
  d.setDate(d.getDate() + n);
  return toISO(d);
};
export const fmtDate = (iso) =>
  parse(iso).toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' });
export const fmtDateLong = (iso) =>
  parse(iso).toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
export const nightsBetween = (a, b) => Math.round((parse(b) - parse(a)) / 86400000);

export const plural = (n, word) => `${n} ${word}${n === 1 ? '' : 's'}`;
