import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { Menu, X, LogOut, Ticket } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  const [open, setOpen] = useState(false);
  const close = () => setOpen(false);

  return (
    <header className="nav">
      <div className="container nav-inner">
        <Link to="/" className="brand" onClick={close}>
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 64 64" width="26" height="26">
              <rect width="64" height="64" rx="14" fill="#2145F5" />
              <path d="M14 36l36-16-10 26-9-9-8 6 2-11z" fill="#FFB627" />
            </svg>
          </span>
          TripEase
        </Link>

        <button className="nav-toggle" aria-label={open ? 'Close menu' : 'Open menu'} aria-expanded={open} onClick={() => setOpen(!open)}>
          {open ? <X size={22} /> : <Menu size={22} />}
        </button>

        <nav className={`nav-links ${open ? 'open' : ''}`} aria-label="Main">
          <Link to="/?mode=flight" onClick={close}>Flights</Link>
          <Link to="/?mode=train" onClick={close}>Trains</Link>
          <Link to="/?mode=bus" onClick={close}>Buses</Link>
          <Link to="/?mode=hotel" onClick={close}>Stays</Link>
          <span className="nav-sep" aria-hidden="true" />
          {user ? (
            <>
              <NavLink to="/bookings" onClick={close} className="nav-trips">
                <Ticket size={16} aria-hidden="true" /> My trips
              </NavLink>
              <button
                className="nav-user"
                onClick={() => {
                  logout();
                  close();
                  nav('/');
                }}
                title="Sign out"
              >
                <span className="avatar">{user.name?.[0]?.toUpperCase()}</span>
                <span className="nav-user-name">{user.name?.split(' ')[0]}</span>
                <LogOut size={15} aria-label="Sign out" />
              </button>
            </>
          ) : (
            <>
              <Link to="/login" onClick={close}>Sign in</Link>
              <Link to="/register" className="btn btn-primary btn-sm" onClick={close}>Create account</Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
