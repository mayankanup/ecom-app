import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

export default function Cart() {
  const { items, updateQuantity, removeFromCart, totalPrice } = useCart();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const handleCheckout = () => {
    if (isAuthenticated) {
      navigate('/checkout');
    } else {
      navigate('/login?redirect=/checkout');
    }
  };

  if (items.length === 0) {
    return (
      <div className="container">
        <h1>Cart</h1>
        <p className="text-muted">Your cart is empty.</p>
        <Link to="/" className="btn btn-primary">
          Browse products
        </Link>
      </div>
    );
  }

  return (
    <div className="container">
      <h1 className="mb-4">Cart</h1>
      <div className="table-responsive">
        <table className="table align-middle">
          <thead>
            <tr>
              <th>Product</th>
              <th>Price</th>
              <th style={{ width: '140px' }}>Quantity</th>
              <th>Subtotal</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.productId}>
                <td className="d-flex align-items-center gap-2">
                  <img
                    src={item.imageUrl}
                    alt={item.name}
                    width="56"
                    height="56"
                    style={{ objectFit: 'cover' }}
                    className="rounded"
                  />
                  <Link to={`/products/${item.productId}`} className="text-decoration-none">
                    {item.name}
                  </Link>
                </td>
                <td>${item.price.toFixed(2)}</td>
                <td>
                  <input
                    type="number"
                    min="1"
                    max={item.stock}
                    className="form-control form-control-sm"
                    value={item.quantity}
                    onChange={(e) => updateQuantity(item.productId, Number(e.target.value))}
                  />
                </td>
                <td>${(item.price * item.quantity).toFixed(2)}</td>
                <td>
                  <button
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => removeFromCart(item.productId)}
                  >
                    Remove
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="d-flex justify-content-end">
        <div className="text-end" style={{ minWidth: '260px' }}>
          <p className="fs-4">
            Total: <strong>${totalPrice.toFixed(2)}</strong>
          </p>
          <button className="btn btn-primary btn-lg" onClick={handleCheckout}>
            Proceed to Checkout
          </button>
        </div>
      </div>
    </div>
  );
}
