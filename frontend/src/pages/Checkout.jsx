import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import apiClient from '../api/client';
import { useCart } from '../context/CartContext';

export default function Checkout() {
  const { items, totalPrice, clearCart } = useCart();
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  const handlePlaceOrder = async () => {
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/api/orders', {
        items: items.map((item) => ({ productId: item.productId, quantity: item.quantity })),
      });
      clearCart();
      navigate('/orders');
    } catch (err) {
      setError(err.response?.data?.message || 'Could not place order. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  if (items.length === 0) {
    return (
      <div className="container">
        <h1>Checkout</h1>
        <p className="text-muted">Your cart is empty.</p>
        <Link to="/" className="btn btn-primary">
          Browse products
        </Link>
      </div>
    );
  }

  return (
    <div className="container" style={{ maxWidth: '600px' }}>
      <h1 className="mb-4">Checkout</h1>
      {error && <div className="alert alert-danger">{error}</div>}
      <ul className="list-group mb-3">
        {items.map((item) => (
          <li key={item.productId} className="list-group-item d-flex justify-content-between">
            <span>
              {item.name} &times; {item.quantity}
            </span>
            <span>${(item.price * item.quantity).toFixed(2)}</span>
          </li>
        ))}
        <li className="list-group-item d-flex justify-content-between fw-bold">
          <span>Total</span>
          <span>${totalPrice.toFixed(2)}</span>
        </li>
      </ul>
      <button className="btn btn-primary btn-lg w-100" onClick={handlePlaceOrder} disabled={submitting}>
        {submitting ? 'Placing order...' : 'Place Order'}
      </button>
    </div>
  );
}
