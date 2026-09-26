import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import api from '../api';

const AuthCtx = createContext(null);
export const useAuth = () => useContext(AuthCtx);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('te_user'));
    } catch {
      return null;
    }
  });

  const persist = (data) => {
    localStorage.setItem('te_token', data.token);
    localStorage.setItem('te_user', JSON.stringify(data.user));
    setUser(data.user);
  };

  const login = useCallback(async (email, password) => {
    const res = await api.post('/api/auth/login', { email, password });
    persist(res.data);
  }, []);

  const register = useCallback(async (name, email, password) => {
    const res = await api.post('/api/auth/register', { name, email, password });
    persist(res.data);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('te_token');
    localStorage.removeItem('te_user');
    setUser(null);
  }, []);

  const value = useMemo(() => ({ user, login, register, logout }), [user, login, register, logout]);
  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>;
}
