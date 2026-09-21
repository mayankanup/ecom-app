import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../api/client';

export default function OrderHistory() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    apiClient
      .get('/api/orders/my')
      .then((response) => setOrders(response.data))
      .catch(() => setError('Could not load your orders.'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <p className="text-center">Loading...</p>;
  }

  if (error) {
    return (
      <div className="container">
        <div className="alert alert-danger">{error}</div>
      </div>
    );
  }

  return (
    <div className="container">
      <h1 className="mb-4">My Orders</h1>
      {orders.length === 0 ? (
        <>
          <p className="text-muted">You haven&apos;t placed any orders yet.</p>
          <Link to="/" className="btn btn-primary">
            Browse products
          </Link>
        </>
      ) : (
        orders.map((order) => (
          <div className="card mb-3" key={order.id}>
            <div className="card-header d-flex justify-content-between">
              <span>
                Order #{order.id} &middot; {new Date(order.createdAt).toLocaleString()}
              </span>
              <span className="badge bg-success align-self-center">{order.status}</span>
            </div>
            <ul className="list-group list-group-flush">
              {order.items.map((item) => (
                <li key={item.productId} className="list-group-item d-flex justify-content-between">
                  <span>
                    {item.productName} &times; {item.quantity}
                  </span>
                  <span>${item.lineTotal.toFixed(2)}</span>
                </li>
              ))}
            </ul>
            <div className="card-footer text-end fw-bold">Total: ${order.total.toFixed(2)}</div>
          </div>
        ))
      )}
    </div>
  );
}
