import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="container page">
      <div className="state">
        <h3>This page has left the station</h3>
        <p>The page you're looking for doesn't exist.</p>
        <Link to="/" className="btn btn-primary">Back to search</Link>
      </div>
    </div>
  );
}
