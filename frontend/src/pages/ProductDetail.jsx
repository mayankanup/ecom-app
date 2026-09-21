import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import apiClient from '../api/client';
import { useCart } from '../context/CartContext';

export default function ProductDetail() {
  const { id } = useParams();
  const [product, setProduct] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [error, setError] = useState(null);
  const { addToCart } = useCart();

  useEffect(() => {
    apiClient
      .get(`/api/products/${id}`)
      .then((response) => setProduct(response.data))
      .catch(() => setError('Product not found.'));
  }, [id]);

  if (error) {
    return (
      <div className="container">
        <div className="alert alert-danger">{error}</div>
        <Link to="/">Back to products</Link>
      </div>
    );
  }

  if (!product) {
    return <p className="text-center">Loading...</p>;
  }

  return (
    <div className="container">
      <Link to="/" className="d-inline-block mb-3">
        &larr; Back to products
      </Link>
      <div className="row g-4">
        <div className="col-md-5">
          <img src={product.imageUrl} alt={product.name} className="img-fluid rounded" />
        </div>
        <div className="col-md-7">
          <h1>{product.name}</h1>
          <p className="text-muted">{product.description}</p>
          <p className="fs-3 fw-bold">${product.price.toFixed(2)}</p>
          <p className="text-muted">{product.stock} in stock</p>
          <div className="d-flex align-items-center gap-2">
            <input
              type="number"
              min="1"
              max={product.stock}
              value={quantity}
              onChange={(e) => setQuantity(Math.max(1, Number(e.target.value)))}
              className="form-control"
              style={{ width: '90px' }}
              disabled={product.stock === 0}
            />
            <button
              className="btn btn-primary"
              disabled={product.stock === 0}
              onClick={() => addToCart(product, quantity)}
            >
              {product.stock === 0 ? 'Out of stock' : 'Add to cart'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
