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
  Image as ImageIcon
} from 'lucide-react';

export const ProductDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToCart, buyNow } = useCart();
  const { isProductOwner } = useAuth();
  const { success, error } = useToast();

  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);

  const fetchProduct = async () => {
    setLoading(true);
    try {
      const res = await productService.getProductById(id);
      const data = res?.data || res;
      setProduct(data);
      setSelectedImageIndex(0);
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

  const isInactive = product.status === 'INACTIVE';
  const isOutOfStock = typeof product.stock === 'number' && product.stock <= 0;

  // Extract all images
  const allImages = [];
  if (product.primaryImageUrl) {
    allImages.push(product.primaryImageUrl);
  }
  if (product.imageUrls && Array.isArray(product.imageUrls)) {
    product.imageUrls.forEach(url => {
      if (!allImages.includes(url)) allImages.push(url);
    });
  }
  if (product.images && Array.isArray(product.images)) {
    product.images.forEach(img => {
      if (img.imageUrl && !allImages.includes(img.imageUrl)) allImages.push(img.imageUrl);
    });
  }

  const activeImage = allImages.length > 0 ? allImages[selectedImageIndex] || allImages[0] : null;

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
    if (isInactive || isOutOfStock) return;
    const finalQty = Math.max(1, quantity);
    addToCart(product, finalQty);
    success(`Đã thêm ${finalQty} sản phẩm "${product.name}" vào giỏ hàng!`);
  };

  const handleBuyNow = () => {
    if (isInactive || isOutOfStock) return;
    const finalQty = Math.max(1, quantity);
    buyNow(product, finalQty);
    success(`Đang chuyển đến trang đặt hàng cho "${product.name}"...`);
    navigate('/checkout');
  };

  return (
    <div className="main-content animate-fade-in" style={{ maxWidth: '1080px', margin: '0 auto', padding: '2rem 1rem' }}>
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
            textDecoration: 'none'
          }}
        >
          <ArrowLeft size={16} /> Quay lại danh mục sản phẩm
        </Link>
      </div>

      {/* Main Product Detail Grid */}
      <div className="card" style={{ padding: '2.5rem', background: 'white', boxShadow: 'var(--shadow-md)', borderRadius: 'var(--radius-xl)' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '2.5rem', alignItems: 'flex-start' }}>
          
          {/* Left: Product Image Gallery */}
          <div>
            {/* Main Visual Box */}
            <div style={{
              height: '380px',
              background: '#f8fafc',
              borderRadius: 'var(--radius-xl)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              position: 'relative',
              overflow: 'hidden',
              border: '1px solid var(--border-color)',
              backgroundColor: '#f1f5f9'
            }}>
              {activeImage ? (
                <img
                  src={activeImage}
                  alt={product.name}
                  style={{
                    width: '100%',
                    height: '100%',
                    objectFit: 'contain',
                    padding: '1rem',
                    transition: 'transform 0.3s ease',
                  }}
                />
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.75rem', color: '#94a3b8' }}>
                  <ImageIcon size={64} />
                  <span style={{ fontSize: '0.9rem', fontWeight: 600 }}>Chưa có hình ảnh sản phẩm</span>
                </div>
              )}
              
              {isInactive && (
                <Badge variant="danger" style={{ position: 'absolute', top: '16px', right: '16px', fontSize: '0.85rem', padding: '0.35rem 0.75rem' }}>
                  Ngừng kinh doanh
                </Badge>
              )}
              {!isInactive && isOutOfStock && (
                <Badge variant="warning" style={{ position: 'absolute', top: '16px', right: '16px', fontSize: '0.85rem', padding: '0.35rem 0.75rem' }}>
                  Hết hàng
                </Badge>
              )}
              {!isInactive && !isOutOfStock && (
                <Badge variant="success" style={{ position: 'absolute', top: '16px', right: '16px', fontSize: '0.85rem', padding: '0.35rem 0.75rem' }}>
                  Sẵn hàng ({product.stock} cái)
                </Badge>
              )}
            </div>

            {/* Thumbnails list if multiple images */}
            {allImages.length > 1 && (
              <div style={{
                display: 'flex',
                gap: '0.75rem',
                marginTop: '1rem',
                overflowX: 'auto',
                paddingBottom: '0.5rem'
              }}>
                {allImages.map((imgUrl, idx) => (
                  <div
                    key={idx}
                    onClick={() => setSelectedImageIndex(idx)}
                    style={{
                      width: '68px',
                      height: '68px',
                      borderRadius: 'var(--radius-md)',
                      overflow: 'hidden',
                      cursor: 'pointer',
                      border: selectedImageIndex === idx ? '2px solid var(--color-primary)' : '1px solid var(--border-color)',
                      boxShadow: selectedImageIndex === idx ? '0 0 0 2px rgba(37,99,235,0.2)' : 'none',
                      backgroundColor: '#f8fafc',
                      flexShrink: 0,
                    }}
                  >
                    <img
                      src={imgUrl}
                      alt={`Thumbnail ${idx + 1}`}
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  </div>
                ))}
              </div>
            )}

            {/* Commitment Badges */}
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
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600 }}>Mã sản phẩm: #{product.id}</span>
            </div>

            <h1 style={{
              fontSize: '1.75rem',
              fontWeight: 800,
              color: 'var(--text-main)',
              lineHeight: 1.3,
              marginBottom: '1rem'
            }}>
              {product.name}
            </h1>

            {/* Pricing Section */}
            <div style={{
              background: '#f8fafc',
              padding: '1.25rem 1.5rem',
              borderRadius: 'var(--radius-lg)',
              border: '1px solid var(--border-color)',
              marginBottom: '1.75rem'
            }}>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Đơn giá niêm yết (Đã bao gồm VAT):</div>
              <div style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--color-primary)' }}>
                {formatCurrency(product.price)}
              </div>
            </div>

            {/* Product Metadata Details */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '2rem', fontSize: '0.925rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '0.5rem', borderBottom: '1px solid var(--border-color)' }}>
                <span style={{ color: 'var(--text-muted)' }}>Tình trạng tồn kho:</span>
                <span style={{ fontWeight: 700, color: product.stock > 0 ? 'var(--color-success)' : 'var(--color-danger)' }}>
                  {product.stock > 0 ? `Còn ${product.stock} sản phẩm` : 'Tạm thời hết hàng'}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '0.5rem', borderBottom: '1px solid var(--border-color)' }}>
                <span style={{ color: 'var(--text-muted)' }}>Hình thức thanh toán:</span>
                <span style={{ fontWeight: 600 }}>VNPay QR / Thẻ nội địa / COD</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '0.5rem', borderBottom: '1px solid var(--border-color)' }}>
                <span style={{ color: 'var(--text-muted)' }}>Quy trình xét duyệt:</span>
                <span style={{ fontWeight: 600 }}>Duyệt PO trước khi thanh toán</span>
              </div>
            </div>

            {/* Actions */}
            {isProductOwner() ? (
              <div style={{ background: '#eff6ff', padding: '1.25rem', borderRadius: 'var(--radius-md)', border: '1px solid #bfdbfe', textAlign: 'center' }}>
                <p style={{ color: '#1e40af', fontSize: '0.9rem', marginBottom: '0.75rem', fontWeight: 600 }}>
                  Bạn đang đăng nhập bằng tài khoản Quản trị / Product Owner
                </p>
                <Link to="/admin/products" className="btn btn-primary" style={{ width: '100%' }}>
                  <Package size={18} /> Đi đến trang Quản lý sản phẩm
                </Link>
              </div>
            ) : isInactive ? (
              <button disabled className="btn btn-secondary btn-lg" style={{ width: '100%', opacity: 0.7 }}>
                <PackageX size={20} /> Sản phẩm đã ngừng kinh doanh
              </button>
            ) : isOutOfStock ? (
              <button disabled className="btn btn-secondary btn-lg" style={{ width: '100%', opacity: 0.7 }}>
                <PackageX size={20} /> Sản phẩm tạm thời hết hàng
              </button>
            ) : (
              <div>
                {/* Quantity Chooser */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.25rem' }}>
                  <span style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--text-main)' }}>Số lượng đặt mua:</span>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', background: '#f8fafc', padding: '0.25rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                    <button
                      type="button"
                      onClick={handleDecrease}
                      disabled={quantity <= 1}
                      className="btn btn-outline btn-sm btn-icon"
                      style={{ width: '32px', height: '32px', padding: 0 }}
                    >
                      <Minus size={16} />
                    </button>
                    <input
                      type="number"
                      min="1"
                      value={quantity}
                      onChange={handleQuantityChange}
                      style={{
                        width: '50px',
                        height: '32px',
                        textAlign: 'center',
                        fontWeight: 700,
                        fontSize: '0.95rem',
                        border: 'none',
                        background: 'transparent',
                        padding: 0
                      }}
                    />
                    <button
                      type="button"
                      onClick={handleIncrease}
                      className="btn btn-outline btn-sm btn-icon"
                      style={{ width: '32px', height: '32px', padding: 0 }}
                    >
                      <Plus size={16} />
                    </button>
                  </div>
                </div>

                {/* Main Action Buttons */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                  <button
                    onClick={handleAddToCart}
                    className="btn btn-outline btn-lg"
                    style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}
                  >
                    <ShoppingCart size={20} /> Thêm vào giỏ
                  </button>
                  <button
                    onClick={handleBuyNow}
                    className="btn btn-primary btn-lg"
                    style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', background: 'linear-gradient(135deg, var(--color-primary) 0%, #1d4ed8 100%)' }}
                  >
                    <Zap size={20} /> Mua ngay
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
