import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useCart } from '../../hooks/useCart';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import orderService from '../../services/order.service';
import { formatCurrency } from '../../utils/formatters';
import { ShoppingBag, ArrowLeft, Send, Save, CheckCircle2 } from 'lucide-react';

export const CheckoutPage = () => {
  const { cartItems, totalAmount, clearCart } = useCart();
  const { user, isAuthenticated } = useAuth();
  const { success, error } = useToast();
  const navigate = useNavigate();

  const [customerId, setCustomerId] = useState(user?.customerId || user?.id || 1);
  const [loading, setLoading] = useState(false);

  if (cartItems.length === 0) {
    return (
      <div className="main-content" style={{ textAlign: 'center', padding: '5rem 1rem' }}>
        <ShoppingBag size={56} color="var(--text-sub)" style={{ margin: '0 auto 1.5rem auto' }} />
        <h2 style={{ fontSize: '1.5rem', fontWeight: 800, marginBottom: '0.5rem' }}>Giỏ hàng của bạn đang trống</h2>
        <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem' }}>Hãy chọn một vài sản phẩm để tiến hành đặt hàng.</p>
        <Link to="/" className="btn btn-primary">
          <ArrowLeft size={16} /> Quay lại mua sắm
        </Link>
      </div>
    );
  }

  const handleCreateOrder = async (isDraft = false) => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: '/checkout' } } });
      return;
    }

    setLoading(true);
    try {
      const orderData = {
        customerId: Number(customerId),
        orderItems: cartItems.map((item) => ({
          productId: item.product.id,
          quantity: item.quantity,
        })),
      };

      let response;
      if (isDraft) {
        response = await orderService.createDraftOrder(orderData);
        success('Đã lưu đơn hàng nháp thành công!');
      } else {
        response = await orderService.createOrder(orderData);
        success('Tạo phiếu mua hàng gửi PO duyệt thành công!');
      }

      clearCart();
      const orderId = response?.id || (response?.data && response?.data?.id);
      if (orderId) {
        navigate(`/orders/${orderId}`);
      } else {
        navigate('/orders');
      }
    } catch (err) {
      error(err.message || 'Có lỗi xảy ra khi tạo đơn hàng. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="main-content animate-fade-in">
      <div style={{ marginBottom: '1.5rem' }}>
        <Link to="/" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-muted)', fontSize: '0.9rem', fontWeight: 600 }}>
          <ArrowLeft size={16} /> Tiếp tục mua sắm
        </Link>
        <h1 className="page-title" style={{ marginTop: '0.5rem' }}>Xác nhận đặt hàng</h1>
      </div>

      <div className="grid-2" style={{ alignItems: 'flex-start' }}>
        {/* Left: Cart Items Review */}
        <div className="card">
          <div className="card-header">
            <h3 className="card-title">
              <ShoppingBag size={20} color="var(--color-primary)" /> Sản phẩm trong đơn ({cartItems.length})
            </h3>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {cartItems.map(({ product, quantity }) => (
              <div
                key={product.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  paddingBottom: '0.75rem',
                  borderBottom: '1px solid var(--border-color)',
                }}
              >
                <div>
                  <h4 style={{ fontSize: '0.95rem', fontWeight: 700 }}>{product.name}</h4>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    {formatCurrency(product.price)} × {quantity}
                  </span>
                </div>
                <span style={{ fontWeight: 800, color: 'var(--text-main)' }}>
                  {formatCurrency(product.price * quantity)}
                </span>
              </div>
            ))}
          </div>

          <div style={{ marginTop: '1.5rem', paddingTop: '1rem', borderTop: '2px dashed var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '1.1rem', fontWeight: 700 }}>Tổng tiền:</span>
            <span style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--color-primary)' }}>
              {formatCurrency(totalAmount)}
            </span>
          </div>
        </div>

        {/* Right: Order Process & Actions */}
        <div className="card">
          <div className="card-header">
            <h3 className="card-title">
              <CheckCircle2 size={20} color="var(--color-success)" /> Thông tin & Quy trình
            </h3>
          </div>

          <div style={{ marginBottom: '1.5rem', fontSize: '0.9rem', lineHeight: 1.6, color: 'var(--text-muted)' }}>
            <div style={{
              background: 'var(--color-info-light)',
              color: '#1e40af',
              padding: '1rem',
              borderRadius: 'var(--radius-md)',
              marginBottom: '1rem'
            }}>
              <strong>Quy trình phê duyệt PO:</strong> Đơn hàng sau khi gửi sẽ được chuyển đến <strong>Product Owner (PO)</strong> để xét duyệt và giữ chỗ tồn kho. Sau khi PO duyệt, bạn sẽ nhận được liên kết thanh toán VNPay.
            </div>

            <div className="form-group">
              <label className="form-label">Customer ID:</label>
              <input
                type="number"
                value={customerId}
                onChange={(e) => setCustomerId(e.target.value)}
                className="form-control"
              />
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <button
              onClick={() => handleCreateOrder(false)}
              disabled={loading}
              className="btn btn-primary btn-lg"
              style={{ width: '100%' }}
            >
              <Send size={18} /> {loading ? 'Đang gửi duyệt...' : 'Gửi duyệt PO (PENDING_APPROVAL)'}
            </button>

            <button
              onClick={() => handleCreateOrder(true)}
              disabled={loading}
              className="btn btn-secondary"
              style={{ width: '100%' }}
            >
              <Save size={18} /> Lưu đơn hàng nháp (DRAFT)
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CheckoutPage;
