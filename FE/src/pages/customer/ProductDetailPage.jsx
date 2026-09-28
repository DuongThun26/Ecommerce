import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import productService from '../../services/product.service';
import { useCart } from '../../hooks/useCart';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { formatCurrency } from '../../utils/formatters';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import {
  ArrowLeft,
  ShoppingCart,
  Zap,
  Package,
  PackageX,
  ShieldCheck,
  Truck,
  FileCheck2,
  Plus,
  Minus,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';

export const ProductDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToCart, buyNow } = useCart();
  const { isProductOwner } = useAuth();
  const { success, warning, error } = useToast();

  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const [buyNowLoading, setBuyNowLoading] = useState(false);

  const fetchProduct = async () => {
    setLoading(true);
    try {
      const res = await productService.getProductById(id);
      const data = res?.data || res;
      setProduct(data);
    } catch (err) {
      error(err.message || 'Không thể tải thông tin chi tiết sản phẩm.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProduct();
  }, [id]);

  if (loading) {
    return <LoadingSpinner fullPage text="Đang tải chi tiết sản phẩm..." />;
  }

  if (!product) {
    return (
      <div className="main-content" style={{ textAlign: 'center', padding: '5rem 1rem' }}>
        <PackageX size={56} color="var(--text-sub)" style={{ margin: '0 auto 1.5rem auto' }} />
        <h2 style={{ fontSize: '1.5rem', fontWeight: 800, marginBottom: '0.5rem' }}>Không tìm thấy sản phẩm #{id}</h2>
        <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem' }}>Sản phẩm không tồn tại hoặc đã bị xóa khỏi hệ thống.</p>
        <Link to="/" className="btn btn-primary">
          <ArrowLeft size={16} /> Quay lại danh sách sản phẩm
        </Link>
      </div>
    );
  }

  const isDiscontinued = product.status === 'DISCONTINUED';
  const isOutOfStock = product.status === 'OUT_OF_STOCK' || (typeof product.stock === 'number' && product.stock <= 0);

  const handleDecrease = () => {
    if (quantity > 1) {
      setQuantity((prev) => prev - 1);
    }
  };

  const handleIncrease = () => {
    setQuantity((prev) => prev + 1);
  };

  const handleQuantityChange = (e) => {
    const val = parseInt(e.target.value, 10);
    if (isNaN(val) || val < 1) {
      setQuantity(1);
    } else {
      setQuantity(val);
    }
  };

  const handleAddToCart = () => {
    if (isDiscontinued) return;
    const finalQty = Math.max(1, quantity);
    addToCart(product, finalQty);
    success(`Đã thêm ${finalQty} sản phẩm "${product.name}" vào giỏ hàng!`);
  };

  const handleBuyNow = () => {
    if (isDiscontinued) return;
    const finalQty = Math.max(1, quantity);
    buyNow(product, finalQty);
    success(`Đang chuyển đến trang đặt hàng cho "${product.name}"...`);
    navigate('/checkout');
  };

  return (
    <div className="main-content animate-fade-in" style={{ maxWidth: '1080px' }}>
      {/* Breadcrumb Navigation */}
      <div style={{ marginBottom: '1.5rem' }}>
        <Link
          to="/"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.4rem',
            color: 'var(--text-muted)',
            fontSize: '0.9rem',
            fontWeight: 600,
            transition: 'color var(--transition-fast)'
          }}
        >
          <ArrowLeft size={16} /> Quay lại danh mục sản phẩm
        </Link>
      </div>

      {/* Main Product Detail Grid */}
      <div className="card" style={{ padding: '2.5rem', background: 'white', boxShadow: 'var(--shadow-md)' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '2.5rem', alignItems: 'flex-start' }}>
          
          {/* Left: Product Visual Box */}
          <div>
            <div style={{
              height: '340px',
              background: 'linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%)',
              borderRadius: 'var(--radius-xl)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              position: 'relative',
              overflow: 'hidden',
              border: '1px solid var(--border-color)',
              boxShadow: 'inset 0 2px 8px rgba(0,0,0,0.02)'
            }}>
              <span style={{ fontSize: '6rem', opacity: 0.85, filter: 'drop-shadow(0 10px 15px rgba(0,0,0,0.1))' }}>📦</span>
              
              {isDiscontinued && (
                <Badge variant="danger" style={{ position: 'absolute', top: '16px', right: '16px', fontSize: '0.85rem', padding: '0.35rem 0.75rem' }}>
                  Ngừng kinh doanh
                </Badge>
              )}
              {!isDiscontinued && isOutOfStock && (
                <Badge variant="warning" style={{ position: 'absolute', top: '16px', right: '16px', fontSize: '0.85rem', padding: '0.35rem 0.75rem' }}>
                  Hết hàng
                </Badge>
              )}
              {!isDiscontinued && !isOutOfStock && (
                <Badge variant="success" style={{ position: 'absolute', top: '16px', right: '16px', fontSize: '0.85rem', padding: '0.35rem 0.75rem' }}>
                  Sẵn hàng ({product.stock} cái)
                </Badge>
              )}
            </div>

            {/* Extra Commitment Badges */}
            <div style={{
              marginTop: '1.5rem',
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              gap: '0.75rem'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: '#f8fafc', padding: '0.75rem', borderRadius: 'var(--radius-md)', fontSize: '0.825rem', color: 'var(--text-main)', border: '1px solid var(--border-color)' }}>
                <ShieldCheck size={18} color="var(--color-primary)" />
                <span>Cam kết chính hãng 100%</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: '#f8fafc', padding: '0.75rem', borderRadius: 'var(--radius-md)', fontSize: '0.825rem', color: 'var(--text-main)', border: '1px solid var(--border-color)' }}>
                <FileCheck2 size={18} color="var(--color-success)" />
                <span>Xuất hóa đơn VAT tự động</span>
              </div>
            </div>
          </div>

          {/* Right: Product Info & Actions */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--color-primary)', background: 'var(--color-primary-light)', padding: '0.15rem 0.5rem', borderRadius: '4px' }}>
                  MÃ SẢN PHẨM: #{product.id}
                </span>
              </div>
              <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-main)', lineHeight: 1.3 }}>
                {product.name}
              </h1>
            </div>

            {/* Price Box */}
            <div style={{
              background: 'linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%)',
              padding: '1.25rem 1.5rem',
              borderRadius: 'var(--radius-lg)',
              border: '1px solid #bbf7d0',
              display: 'flex',
              flexDirection: 'column',
              gap: '0.25rem'
            }}>
              <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#166534' }}>
                Đơn giá niêm yết:
              </div>
              <div style={{ fontSize: '2.2rem', fontWeight: 800, color: 'var(--color-primary)' }}>
                {formatCurrency(product.price)}
              </div>
              <div style={{ fontSize: '0.8rem', color: '#15803d' }}>
                ✓ Đã bao gồm thuế GTGT & tự động đồng bộ hóa đơn điện tử khi thanh toán.
              </div>
            </div>

            {/* Inventory Status Note */}
            <div style={{ fontSize: '0.9rem', color: 'var(--text-muted)', lineHeight: 1.6 }}>
              <strong>Tình trạng: </strong>
              {isDiscontinued ? (
                <span style={{ color: 'var(--color-danger)', fontWeight: 700 }}>Ngừng kinh doanh</span>
              ) : isOutOfStock ? (
                <span style={{ color: '#b45309', fontWeight: 700 }}>Tạm hết hàng (Vẫn có thể đặt đơn giữ chỗ)</span>
              ) : (
                <span style={{ color: 'var(--color-success)', fontWeight: 700 }}>Còn hàng ({product.stock} sản phẩm trong kho)</span>
              )}
            </div>

            {/* Actions for PO or Customer/Admin */}
            {isProductOwner() ? (
              <div style={{ marginTop: '1rem', background: '#f8fafc', padding: '1.25rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
                  Bạn đang đăng nhập dưới quyền <strong>Product Owner</strong>. Bạn có quyền quản lý và cập nhật thông tin sản phẩm này.
                </p>
                <Link to="/admin/products" className="btn btn-outline" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Package size={16} /> Đến trang Quản lý Sản phẩm
                </Link>
              </div>
            ) : isDiscontinued ? (
              <div style={{ marginTop: '1rem' }}>
                <button disabled className="btn btn-secondary btn-lg" style={{ width: '100%', opacity: 0.7 }}>
                  <PackageX size={18} /> Sản phẩm đã ngừng kinh doanh
                </button>
              </div>
            ) : (
              <div style={{ marginTop: '1rem', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                {/* Quantity Selector */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                  <span style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--text-main)' }}>Số lượng đặt mua:</span>
                  
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', background: '#f8fafc', padding: '0.25rem 0.5rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                    <button
                      type="button"
                      onClick={handleDecrease}
                      disabled={quantity <= 1}
                      className="btn btn-outline btn-sm btn-icon"
                      style={{ width: '32px', height: '32px', padding: 0 }}
                      title="Giảm số lượng"
                    >
                      <Minus size={15} />
                    </button>
                    <input
                      type="number"
                      min="1"
                      value={quantity}
                      onChange={handleQuantityChange}
                      style={{
                        width: '56px',
                        height: '32px',
                        textAlign: 'center',
                        fontWeight: 700,
                        fontSize: '1rem',
                        border: '1px solid var(--border-color)',
                        borderRadius: 'var(--radius-sm)',
                        padding: 0
                      }}
                    />
                    <button
                      type="button"
                      onClick={handleIncrease}
                      className="btn btn-outline btn-sm btn-icon"
                      style={{ width: '32px', height: '32px', padding: 0 }}
                      title="Tăng số lượng"
                    >
                      <Plus size={15} />
                    </button>
                  </div>
                </div>

                {/* Total Preview */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 1rem', background: '#f8fafc', borderRadius: 'var(--radius-md)' }}>
                  <span style={{ fontSize: '0.9rem', color: 'var(--text-muted)', fontWeight: 600 }}>Thành tiền tạm tính:</span>
                  <span style={{ fontSize: '1.3rem', fontWeight: 800, color: 'var(--color-primary)' }}>
                    {formatCurrency(product.price * quantity)}
                  </span>
                </div>

                {/* Action Buttons: Add to Cart & Buy Now */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                  <button
                    type="button"
                    onClick={handleAddToCart}
                    className="btn btn-outline btn-lg"
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '0.5rem',
                      borderColor: 'var(--color-primary)',
                      color: 'var(--color-primary)',
                      background: 'var(--color-primary-light)',
                      fontWeight: 700
                    }}
                  >
                    <ShoppingCart size={18} /> Thêm vào giỏ hàng
                  </button>

                  <button
                    type="button"
                    onClick={handleBuyNow}
                    className="btn btn-primary btn-lg"
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '0.5rem',
                      fontWeight: 700
                    }}
                  >
                    <Zap size={18} /> Đặt hàng ngay
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProductDetailPage;
