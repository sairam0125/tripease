import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { errMsg } from '../api';
import { useAuth } from '../context/AuthContext';

export default function Register() {
  const { register } = useAuth();
  const nav = useNavigate();
  const location = useLocation();
  const from = location.state?.from;
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    if (password.length < 6) return setError('Password must be at least 6 characters.');
    setBusy(true);
    try {
      await register(name, email, password);
      nav(from ? `${from.pathname}${from.search || ''}` : '/', { replace: true });
    } catch (err) {
      setError(errMsg(err));
      setBusy(false);
    }
  };

  return (
    <div className="container page">
      <form className="auth-card" onSubmit={submit}>
        <h2>Create your account</h2>
        <p className="muted">It takes a few seconds. No payment details needed.</p>
        <label className="field field-wide">
          <span>Full name</span>
          <input value={name} onChange={(e) => setName(e.target.value)} maxLength={80} autoComplete="name" required />
        </label>
        <label className="field field-wide">
          <span>Email</span>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
        </label>
        <label className="field field-wide">
          <span>Password (6+ characters)</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" minLength={6} required />
        </label>
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="btn btn-primary btn-lg" disabled={busy}>{busy ? 'Creating...' : 'Create account'}</button>
        <p className="muted center">Already registered? <Link to="/login" state={{ from }} className="btn-link">Sign in</Link></p>
      </form>
    </div>
  );
}
