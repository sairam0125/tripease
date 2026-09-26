import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { errMsg } from '../api';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const { login } = useAuth();
  const nav = useNavigate();
  const location = useLocation();
  const from = location.state?.from;
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await login(email, password);
      nav(from ? `${from.pathname}${from.search || ''}` : '/bookings', { replace: true });
    } catch (err) {
      setError(errMsg(err));
      setBusy(false);
    }
  };

  return (
    <div className="container page">
      <form className="auth-card" onSubmit={submit}>
        <h2>Welcome back</h2>
        <p className="muted">Sign in to book and manage your trips.</p>
        <label className="field field-wide">
          <span>Email</span>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
        </label>
        <label className="field field-wide">
          <span>Password</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
        </label>
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="btn btn-primary btn-lg" disabled={busy}>{busy ? 'Signing in...' : 'Sign in'}</button>
        <p className="muted center">New here? <Link to="/register" state={{ from }} className="btn-link">Create an account</Link></p>
      </form>
    </div>
  );
}
