import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { formatCurrency } from '../../utils/formatters';
import { useCart } from '../../hooks/useCart';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { ShoppingCart, PackageX, Package, Plus, Minus } from 'lucide-react';
import Badge from '../common/Badge';

export const ProductCard = ({ product }) => {
  const { addToCart } = useCart();
  const { isAdmin, isProductOwner } = useAuth();
  const { success, warning } = useToast();
  const [quantity, setQuantity] = useState(1);

  const currentStock = typeof product.stock === 'number' ? product.stock : 999;
  const isOutOfStock = typeof product.stock === 'number' ? product.stock <= 0 : false;

  const handleDecrease = () => {
    if (quantity > 1) {
      setQuantity((prev) => prev - 1);
    }
  };

  const handleIncrease = () => {
    if (quantity < currentStock) {
      setQuantity((prev) => prev + 1);
    } else {
      warning(`Số lượng tối đa có thể mua là ${currentStock}`);
    }
  };

  const handleQuantityChange = (e) => {
    const val = parseInt(e.target.value, 10);
    if (isNaN(val) || val < 1) {
      setQuantity(1);
    } else if (val > currentStock) {
      setQuantity(currentStock);
      warning(`Số lượng tồn kho chỉ còn ${currentStock}`);
    } else {
      setQuantity(val);
    }
  };

  const handleAddToCart = () => {
    if (isOutOfStock) return;
    const finalQty = Math.min(Math.max(1, quantity), currentStock);
    addToCart(product, finalQty);
    success(`Đã thêm ${finalQty} sản phẩm "${product.name}" vào giỏ hàng!`);
    setQuantity(1);
  };

  return (
    <div className="card card-hover" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Product Image Placeholder / Visual Box */}
      <div style={{
        height: '180px',
        background: 'linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%)',
        borderRadius: 'var(--radius-md)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: '1rem',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <span style={{ fontSize: '3rem', opacity: 0.8 }}>📦</span>
        {product.stock <= 5 && product.stock > 0 && (
          <Badge variant="warning" style={{ position: 'absolute', top: '10px', right: '10px' }}>
            Còn {product.stock} sp
          </Badge>
        )}
        {isOutOfStock && (
          <Badge variant="danger" style={{ position: 'absolute', top: '10px', right: '10px' }}>
            Hết hàng
          </Badge>
        )}
      </div>

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <h4 style={{
          fontSize: '1.05rem',
          fontWeight: 700,
          color: 'var(--text-main)',
          marginBottom: '0.5rem',
          display: '-webkit-box',
          WebkitLineClamp: 2,
          WebkitBoxOrient: 'vertical',
          overflow: 'hidden',
          minHeight: '2.8rem'
        }}>
          {product.name}
        </h4>

        <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.5rem', marginTop: 'auto', marginBottom: '1rem' }}>
          <span style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--color-primary)' }}>
            {formatCurrency(product.price)}
          </span>
        </div>

        {isProductOwner() || isAdmin() ? (
          <Link
            to="/admin/products"
            className="btn btn-outline"
            style={{
              width: '100%',
              marginTop: 'auto',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.5rem',
              color: 'var(--color-primary)',
              borderColor: 'var(--border-focus)'
            }}
          >
            <Package size={18} /> Quản lý sản phẩm
          </Link>
        ) : isOutOfStock ? (
          <button disabled className="btn btn-secondary" style={{ width: '100%', marginTop: 'auto' }}>
            <PackageX size={18} /> Tạm hết hàng
          </button>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.625rem', marginTop: 'auto' }}>
            {/* Quantity Selector */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: '#f8fafc', padding: '0.35rem 0.5rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
              <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)' }}>Số lượng:</span>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                <button
                  type="button"
                  onClick={handleDecrease}
                  disabled={quantity <= 1}
                  className="btn btn-outline btn-sm btn-icon"
                  style={{ width: '28px', height: '28px', padding: 0 }}
                  title="Giảm số lượng"
                >
                  <Minus size={14} />
                </button>
                <input
                  type="number"
                  min="1"
                  max={product.stock}
                  value={quantity}
                  onChange={handleQuantityChange}
                  style={{
                    width: '45px',
                    height: '28px',
                    textAlign: 'center',
                    fontWeight: 700,
                    fontSize: '0.9rem',
                    border: '1px solid var(--border-color)',
                    borderRadius: 'var(--radius-sm)',
                    padding: 0
                  }}
                />
                <button
                  type="button"
                  onClick={handleIncrease}
                  disabled={quantity >= product.stock}
                  className="btn btn-outline btn-sm btn-icon"
                  style={{ width: '28px', height: '28px', padding: 0 }}
                  title="Tăng số lượng"
                >
                  <Plus size={14} />
                </button>
              </div>
            </div>

            {/* Add to Cart Button */}
            <button
              onClick={handleAddToCart}
              className="btn btn-primary"
              style={{ width: '100%' }}
            >
              <ShoppingCart size={18} /> Thêm vào giỏ
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default ProductCard;
