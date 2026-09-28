import React from 'react';
import { formatCurrency } from '../../utils/formatters';
import { useCart } from '../../hooks/useCart';
import { useToast } from '../../hooks/useToast';
import { ShoppingCart, Check, PackageX } from 'lucide-react';
import Badge from '../common/Badge';

export const ProductCard = ({ product }) => {
  const { addToCart } = useCart();
  const { success } = useToast();

  const isOutOfStock = product.stock === 0;

  const handleAddToCart = () => {
    if (isOutOfStock) return;
    addToCart(product, 1);
    success(`Đã thêm "${product.name}" vào giỏ hàng!`);
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

        <button
          onClick={handleAddToCart}
          disabled={isOutOfStock}
          className={`btn ${isOutOfStock ? 'btn-secondary' : 'btn-primary'}`}
          style={{ width: '100%', marginTop: 'auto' }}
        >
          {isOutOfStock ? (
            <>
              <PackageX size={18} /> Tạm hết hàng
            </>
          ) : (
            <>
              <ShoppingCart size={18} /> Thêm vào giỏ
            </>
          )}
        </button>
      </div>
    </div>
  );
};

export default ProductCard;
