import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  timeout: 45000, // free hosting can take ~30s to wake up
});

// Attach the JWT to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('te_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// If the token expired, sign the user out and send them to the login page
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const url = err.config?.url || '';
    if (err.response?.status === 401 && localStorage.getItem('te_token') && !url.includes('/api/auth/')) {
      localStorage.removeItem('te_token');
      localStorage.removeItem('te_user');
      window.location.assign('/login');
    }
    return Promise.reject(err);
  }
);

export const errMsg = (e) =>
  e?.response?.data?.message ||
  (e?.code === 'ECONNABORTED' ? 'The server is taking too long to respond. Please try again.' : null) ||
  (e?.message === 'Network Error' ? 'Cannot reach the server. Check your connection and try again.' : null) ||
  'Something went wrong. Please try again.';

export default api;
