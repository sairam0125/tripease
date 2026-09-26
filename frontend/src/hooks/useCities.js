import { useEffect, useState } from 'react';
import api from '../api';

// Used until the API answers (e.g. while a free-tier server is waking up)
const FALLBACK = [
  'Ahmedabad', 'Bengaluru', 'Chennai', 'Delhi', 'Goa', 'Hyderabad',
  'Jaipur', 'Kochi', 'Kolkata', 'Mumbai', 'Pune', 'Visakhapatnam',
];

let cache = null;

export function useCities() {
  const [cities, setCities] = useState(cache || FALLBACK);
  useEffect(() => {
    if (cache) return;
    api
      .get('/api/cities')
      .then((res) => {
        cache = res.data.map((c) => c.name);
        setCities(cache);
      })
      .catch(() => {});
  }, []);
  return cities;
}
