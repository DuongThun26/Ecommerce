import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { formatCurrency } from '../../utils/formatters';
import { useCart } from '../../hooks/useCart';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { ShoppingCart, PackageX, Package, Plus, Minus, Image as ImageIcon, Eye } from 'lucide-react';
import Badge from '../common/Badge';

export const ProductCard = ({ product }) => {
  const { addToCart } = useCart();
  const { isProductOwner } = useAuth();
  const { success } = useToast();
  const [quantity, setQuantity] = useState(1);
  const [imageError, setImageError] = useState(false);

  const isInactive = product.status === 'INACTIVE';
  const isOutOfStock = typeof product.stock === 'number' && product.stock <= 0;

  const displayImage = !imageError && (
    product.primaryImageUrl ||
    (product.imageUrls && product.imageUrls[0]) ||
    (product.images && product.images[0]?.imageUrl)
  );

  const totalImages = (product.imageUrls?.length) || (product.images?.length) || (product.primaryImageUrl ? 1 : 0);

  const handleDecrease = (e) => {
    e.stopPropagation();
    if (quantity > 1) {
      setQuantity((prev) => prev - 1);
    }
  };

  const handleIncrease = (e) => {
    e.stopPropagation();
    setQuantity((prev) => prev + 1);
  };

  const handleQuantityChange = (e) => {
    e.stopPropagation();
    const val = parseInt(e.target.value, 10);
    if (isNaN(val) || val < 1) {
      setQuantity(1);
    } else {
      setQuantity(val);
    }
  };

  const handleAddToCart = (e) => {
    e.stopPropagation();
    if (isInactive || isOutOfStock) return;
    const finalQty = Math.max(1, quantity);
    addToCart(product, finalQty);
    success(`Đã thêm ${finalQty} sản phẩm "${product.name}" vào giỏ hàng!`);
    setQuantity(1);
  };

  return (
    <div className="card card-hover" style={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
      {/* Product Image Link Box */}
      <Link
        to={`/products/${product.id}`}
        style={{
          display: 'block',
          height: '200px',
          background: '#f8fafc',
          borderRadius: 'var(--radius-md)',
          position: 'relative',
          overflow: 'hidden',
          backgroundColor: '#f1f5f9',
          marginBottom: '1rem',
          textDecoration: 'none',
          color: 'inherit'
        }}
        title={`Xem chi tiết ${product.name}`}
      >
        {displayImage ? (
          <img
            src={displayImage}
            alt={product.name}
            onError={() => setImageError(true)}
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              transition: 'transform 0.3s ease',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.transform = 'scale(1.05)'; }}
            onMouseLeave={(e) => { e.currentTarget.style.transform = 'scale(1)'; }}
          />
        ) : (
          <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', color: '#94a3b8' }}>
            <ImageIcon size={40} />
            <span style={{ fontSize: '0.75rem', fontWeight: 600 }}>Chưa có ảnh</span>
          </div>
        )}

        {/* Multiple images indicator */}
        {totalImages > 1 && (
          <div style={{
            position: 'absolute',
            bottom: '8px',
            right: '8px',
            backgroundColor: 'rgba(15, 23, 42, 0.75)',
            backdropFilter: 'blur(4px)',
            color: 'white',
            fontSize: '0.7rem',
            fontWeight: 700,
            padding: '2px 8px',
            borderRadius: '12px',
          }}>
            +{totalImages} ảnh
          </div>
        )}

        {/* Status Badges */}
        {isInactive && (
          <Badge variant="danger" style={{ position: 'absolute', top: '10px', right: '10px' }}>
            Ngừng bán
          </Badge>
        )}
        {!isInactive && isOutOfStock && (
          <Badge variant="warning" style={{ position: 'absolute', top: '10px', right: '10px' }}>
            Hết hàng
          </Badge>
        )}
        {!isInactive && !isOutOfStock && product.stock <= 5 && product.stock > 0 && (
          <Badge variant="neutral" style={{ position: 'absolute', top: '10px', right: '10px' }}>
            Còn {product.stock} sp
          </Badge>
        )}
      </Link>

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
          <Link
            to={`/products/${product.id}`}
            style={{ color: 'inherit', textDecoration: 'none' }}
            title={`Xem chi tiết ${product.name}`}
          >
            {product.name}
          </Link>
        </h4>

        <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.5rem', marginTop: 'auto', marginBottom: '1rem' }}>
          <span style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--color-primary)' }}>
            {formatCurrency(product.price)}
          </span>
        </div>

        {isProductOwner() ? (
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
        ) : isInactive ? (
          <button disabled className="btn btn-secondary" style={{ width: '100%', marginTop: 'auto', opacity: 0.7 }}>
            <PackageX size={18} /> Ngừng kinh doanh
          </button>
        ) : isOutOfStock ? (
          <button disabled className="btn btn-secondary" style={{ width: '100%', marginTop: 'auto', opacity: 0.7 }}>
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
                  className="btn btn-outline btn-sm btn-icon"
                  style={{ width: '28px', height: '28px', padding: 0 }}
                  title="Tăng số lượng"
                >
                  <Plus size={14} />
                </button>
              </div>
            </div>

            {/* Action Buttons: Add to Cart & View Details */}
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <Link
                to={`/products/${product.id}`}
                className="btn btn-outline btn-sm"
                style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.25rem' }}
                title="Xem chi tiết"
              >
                <Eye size={16} /> Chi tiết
              </Link>
              <button
                onClick={handleAddToCart}
                className="btn btn-primary btn-sm"
                style={{ flex: 2, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.35rem' }}
              >
                <ShoppingCart size={16} /> Thêm giỏ
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ProductCard;
