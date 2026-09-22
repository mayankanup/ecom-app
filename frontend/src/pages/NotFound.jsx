import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="container text-center py-5">
      <h1 className="display-4">404</h1>
      <p className="text-muted mb-4">We couldn&apos;t find the page you were looking for.</p>
      <Link to="/" className="btn btn-primary">
        Back to products
      </Link>
    </div>
  );
}
