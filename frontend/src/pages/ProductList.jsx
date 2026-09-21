import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../api/client';
import { useCart } from '../context/CartContext';

export default function ProductList() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { addToCart } = useCart();

  useEffect(() => {
    apiClient
      .get('/api/products')
      .then((response) => setProducts(response.data))
      .catch(() => setError('Could not load products. Is the backend running?'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <p className="text-center">Loading products...</p>;
  }

  if (error) {
    return <div className="alert alert-danger">{error}</div>;
  }

  return (
    <div className="container">
      <h1 className="mb-4">Products</h1>
      <div className="row row-cols-1 row-cols-sm-2 row-cols-lg-4 g-4">
        {products.map((product) => (
          <div className="col" key={product.id}>
            <div className="card h-100">
              <img
                src={product.imageUrl}
                className="card-img-top"
                alt={product.name}
                style={{ objectFit: 'cover', height: '180px' }}
              />
              <div className="card-body d-flex flex-column">
                <h5 className="card-title">
                  <Link to={`/products/${product.id}`} className="text-decoration-none">
                    {product.name}
                  </Link>
                </h5>
                <p className="card-text text-muted small flex-grow-1">{product.description}</p>
                <p className="fw-bold">${product.price.toFixed(2)}</p>
                <button
                  className="btn btn-primary mt-auto"
                  disabled={product.stock === 0}
                  onClick={() => addToCart(product, 1)}
                >
                  {product.stock === 0 ? 'Out of stock' : 'Add to cart'}
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
