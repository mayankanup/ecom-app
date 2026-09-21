import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

export default function NavBar() {
  const { user, isAuthenticated, logout } = useAuth();
  const { totalItems } = useCart();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="navbar navbar-expand-md navbar-dark bg-dark mb-4">
      <div className="container">
        <Link className="navbar-brand" to="/">
          ecom-app
        </Link>
        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#navbarContent"
        >
          <span className="navbar-toggler-icon"></span>
        </button>
        <div className="collapse navbar-collapse" id="navbarContent">
          <ul className="navbar-nav me-auto">
            <li className="nav-item">
              <Link className="nav-link" to="/">
                Products
              </Link>
            </li>
            {isAuthenticated && (
              <li className="nav-item">
                <Link className="nav-link" to="/orders">
                  My Orders
                </Link>
              </li>
            )}
          </ul>
          <ul className="navbar-nav align-items-md-center">
            <li className="nav-item">
              <Link className="nav-link" to="/cart">
                Cart
                {totalItems > 0 && (
                  <span className="badge bg-primary rounded-pill ms-1">{totalItems}</span>
                )}
              </Link>
            </li>
            {isAuthenticated ? (
              <>
                <li className="nav-item">
                  <span className="nav-link text-white-50">Hi, {user.name}</span>
                </li>
                <li className="nav-item">
                  <button className="btn btn-outline-light btn-sm ms-md-2" onClick={handleLogout}>
                    Logout
                  </button>
                </li>
              </>
            ) : (
              <li className="nav-item">
                <Link className="btn btn-outline-light btn-sm ms-md-2" to="/login">
                  Login
                </Link>
              </li>
            )}
          </ul>
        </div>
      </div>
    </nav>
  );
}
