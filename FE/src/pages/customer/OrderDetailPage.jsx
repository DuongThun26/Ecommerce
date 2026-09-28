import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import orderService from '../../services/order.service';
import paymentService from '../../services/payment.service';
import invoiceService from '../../services/invoice.service';
import { useToast } from '../../hooks/useToast';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import { ORDER_STATUS, ORDER_STATUS_LABELS, ORDER_STATUS_BADGE } from '../../utils/constants';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import OrderTimeline from '../../components/customer/OrderTimeline';
import {
  ArrowLeft,
  CreditCard,
  Receipt,
  Send,
  XCircle,
  FileText,
  Clock,
  ShieldCheck,
  AlertTriangle
} from 'lucide-react';

export const OrderDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { success, error } = useToast();

  const [order, setOrder] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchOrderDetail = async () => {
    setLoading(true);
    try {
      const orderData = await orderService.getOrderById(id);
      setOrder(orderData);
      try {
        const historyData = await orderService.getOrderStatusHistory(id);
        setHistory(Array.isArray(historyData) ? historyData : []);
      } catch {
        setHistory([]);
      }
    } catch (err) {
      error(err.message || 'Không thể tải thông tin đơn hàng.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrderDetail();
  }, [id]);

  const handlePayVnPay = async () => {
    setActionLoading(true);
    try {
      const res = await paymentService.createVnPayUrl(order.id);
      if (res && res.paymentUrl) {
        window.location.href = res.paymentUrl;
      } else {
        error('Không tạo được liên kết thanh toán VNPay.');
      }
    } catch (err) {
      error(err.message || 'Lỗi khi khởi tạo thanh toán VNPay.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleSubmitOrder = async () => {
    setActionLoading(true);
    try {
      await orderService.submitOrder(order.id);
      success('Đã nộp đơn hàng lên Product Owner phê duyệt!');
      fetchOrderDetail();
    } catch (err) {
      error(err.message || 'Lỗi khi nộp đơn hàng.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCancelOrder = async () => {
    if (!window.confirm('Bạn có chắc chắn muốn hủy đơn hàng này?')) return;
    setActionLoading(true);
    try {
      await orderService.cancelOrder(order.id, 'Khách hàng yêu cầu hủy đơn');
      success('Đã hủy đơn hàng thành công.');
      fetchOrderDetail();
    } catch (err) {
      error(err.message || 'Lỗi khi hủy đơn hàng.');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return <LoadingSpinner fullPage text="Đang tải chi tiết đơn hàng..." />;
  }

  if (!order) {
    return (
      <div className="main-content" style={{ textAlign: 'center', padding: '4rem 1rem' }}>
        <h2>Không tìm thấy đơn hàng #{id}</h2>
        <Link to="/orders" className="btn btn-primary" style={{ marginTop: '1rem' }}>
          Quay lại danh sách
        </Link>
      </div>
    );
  }

  const isPaid = order.status === ORDER_STATUS.PAID || order.status === ORDER_STATUS.COMPLETED;
  const isPaymentPending = order.status === ORDER_STATUS.PAYMENT_PENDING;
  const canCancel = [ORDER_STATUS.DRAFT, ORDER_STATUS.PENDING_APPROVAL, ORDER_STATUS.PAYMENT_PENDING, ORDER_STATUS.RETURNED].includes(order.status);
  const canSubmit = order.status === ORDER_STATUS.DRAFT || order.status === ORDER_STATUS.RETURNED;

  return (
    <div className="main-content animate-fade-in">
      {/* Top bar navigation */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <Link to="/orders" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-muted)', fontSize: '0.9rem', fontWeight: 600 }}>
          <ArrowLeft size={16} /> Danh sách đơn hàng
        </Link>

        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <Badge variant={ORDER_STATUS_BADGE[order.status] || 'neutral'}>
            {ORDER_STATUS_LABELS[order.status] || order.status}
          </Badge>

          {isPaid && (
            <Link to={`/invoices`} className="btn btn-sm btn-success">
              <Receipt size={16} /> Xem hóa đơn VAT
            </Link>
          )}
        </div>
      </div>

      <div className="page-header" style={{ marginBottom: '1.5rem' }}>
        <div>
          <h1 className="page-title">Chi tiết đơn hàng #{order.id}</h1>
          <p className="page-subtitle">Ngày đặt: {formatDateTime(order.createdAt)}</p>
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          {canSubmit && (
            <button
              onClick={handleSubmitOrder}
              disabled={actionLoading}
              className="btn btn-primary"
            >
              <Send size={16} /> Nộp PO duyệt lại
            </button>
          )}

          {isPaymentPending && (
            <button
              onClick={handlePayVnPay}
              disabled={actionLoading}
              className="btn btn-primary"
              style={{ background: 'linear-gradient(135deg, #0284c7, #0369a1)' }}
            >
              <CreditCard size={16} /> Thanh toán VNPay ngay
            </button>
          )}

          {canCancel && (
            <button
              onClick={handleCancelOrder}
              disabled={actionLoading}
              className="btn btn-danger"
            >
              <XCircle size={16} /> Hủy đơn hàng
            </button>
          )}
        </div>
      </div>

      {/* PO Review Feedback Box if Returned or Rejected */}
      {(order.returnedReason || order.rejectedReason) && (
        <div style={{
          background: order.rejectedReason ? 'var(--color-danger-light)' : 'var(--color-warning-light)',
          color: order.rejectedReason ? 'var(--color-danger)' : '#b45309',
          padding: '1.25rem',
          borderRadius: 'var(--radius-md)',
          marginBottom: '1.5rem',
          display: 'flex',
          gap: '0.75rem',
          alignItems: 'flex-start',
        }}>
          <AlertTriangle size={20} style={{ flexShrink: 0, marginTop: '2px' }} />
          <div>
            <strong>Phản hồi từ Product Owner ({order.approvedBy ? order.approvedBy.username : 'PO'}):</strong>
            <p style={{ marginTop: '0.25rem', fontSize: '0.9rem' }}>
              {order.returnedReason || order.rejectedReason}
            </p>
          </div>
        </div>
      )}

      <div className="grid-2" style={{ alignItems: 'flex-start' }}>
        {/* Left: Product Items Table */}
        <div className="card">
          <div className="card-header">
            <h3 className="card-title">
              <FileText size={20} color="var(--color-primary)" /> Danh sách sản phẩm
            </h3>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {order.orderItems?.map((item) => (
              <div
                key={item.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  paddingBottom: '0.75rem',
                  borderBottom: '1px solid var(--border-color)',
                }}
              >
                <div>
                  <h4 style={{ fontSize: '0.95rem', fontWeight: 700 }}>
                    {item.product?.name || item.productName || `Sản phẩm #${item.productId || ''}`}
                  </h4>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    {formatCurrency(item.unitPrice)} × {item.quantity}
                  </span>
                </div>
                <span style={{ fontWeight: 800, color: 'var(--text-main)' }}>
                  {formatCurrency(item.subTotal || item.subtotal || (item.unitPrice * item.quantity))}
                </span>
              </div>
            ))}
          </div>

          <div style={{ marginTop: '1.5rem', paddingTop: '1rem', borderTop: '2px dashed var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '1.1rem', fontWeight: 700 }}>Tổng thanh toán:</span>
            <span style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--color-primary)' }}>
              {formatCurrency(order.totalAmount)}
            </span>
          </div>
        </div>

        {/* Right: Status Timeline & Approval details */}
        <div className="card">
          <div className="card-header">
            <h3 className="card-title">
              <Clock size={20} color="var(--color-secondary)" /> Tiến trình & Lịch sử đơn hàng
            </h3>
          </div>

          <OrderTimeline history={history} />
        </div>
      </div>
    </div>
  );
};

export default OrderDetailPage;
