import { SearchX, AlertTriangle } from 'lucide-react';

export function ListSkeleton({ rows = 3 }) {
  return (
    <div className="stack" aria-busy="true" aria-label="Loading results">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="skeleton skeleton-card" />
      ))}
    </div>
  );
}

export function EmptyState({ title, children }) {
  return (
    <div className="state">
      <SearchX size={28} aria-hidden="true" />
      <h3>{title}</h3>
      <p>{children}</p>
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="state state-error" role="alert">
      <AlertTriangle size={28} aria-hidden="true" />
      <h3>We couldn't load this</h3>
      <p>{message}</p>
      {onRetry && (
        <button className="btn btn-outline" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}
