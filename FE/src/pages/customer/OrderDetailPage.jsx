import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import orderService from '../../services/order.service';
import productService from '../../services/product.service';
import paymentService from '../../services/payment.service';
import invoiceService from '../../services/invoice.service';
import { useToast } from '../../hooks/useToast';
import { useAuth } from '../../hooks/useAuth';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import { ORDER_STATUS, ORDER_STATUS_LABELS, ORDER_STATUS_BADGE } from '../../utils/constants';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Modal from '../../components/common/Modal';
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
  AlertTriangle,
  CheckCircle2,
  RefreshCw,
  Check,
  Plus,
  Minus,
  Trash2,
  Edit3
} from 'lucide-react';

export const OrderDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { success, error } = useToast();

  const { user, isCustomer, isAdmin, isProductOwner } = useAuth();
  const [order, setOrder] = useState(null);
  const [payment, setPayment] = useState(null);
  const [history, setHistory] = useState([]);
  const [invoice, setInvoice] = useState(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const [subReviewModal, setSubReviewModal] = useState({
    isOpen: false,
    subOrder: null,
    action: 'APPROVE',
    reason: '',
  });

  const [addItemModal, setAddItemModal] = useState({
    isOpen: false,
    productId: '',
    quantity: 1,
    products: [],
    loadingProducts: false,
  });

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
      try {
        const payData = await paymentService.getPaymentByOrderId(id);
        setPayment(payData);
      } catch {
        setPayment(null);
      }
      if (orderData.status === ORDER_STATUS.PAID || orderData.status === ORDER_STATUS.COMPLETED) {
        try {
          const invData = await invoiceService.getInvoiceByOrderId(id);
          setInvoice(invData);
        } catch {
          setInvoice(null);
        }
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
    const isPaidOrder = order.status === ORDER_STATUS.PAID || (payment && payment.status === 'SUCCESS');
    const confirmMsg = isPaidOrder
      ? `Đơn hàng #${order.id} đã được thanh toán (${formatCurrency(order.totalAmount)}).\n\nBạn có chắc chắn muốn hủy đơn hàng này? Hệ thống sẽ thực hiện hoàn trả lại toàn bộ số tiền về tài khoản/thẻ thanh toán của bạn.`
      : 'Bạn có chắc chắn muốn hủy đơn hàng này?';

    if (!window.confirm(confirmMsg)) return;

    setActionLoading(true);
    try {
      await orderService.cancelOrder(order.id, 'Khách hàng yêu cầu hủy đơn');
      success(isPaidOrder ? 'Đã hủy đơn hàng và thực hiện hoàn tiền thành công!' : 'Đã hủy đơn hàng thành công.');
      fetchOrderDetail();
    } catch (err) {
      error(err.message || 'Lỗi khi hủy đơn hàng.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDecision = async (action) => {
    const isAccept = action === 'ACCEPT_PARTIAL';
    const confirmMsg = isAccept
      ? 'Bạn có đồng ý tiếp tục mua các sản phẩm được duyệt và loại bỏ các sản phẩm bị từ chối khỏi đơn hàng?'
      : 'Bạn có chắc chắn muốn hủy toàn bộ đơn hàng này?';

    if (!window.confirm(confirmMsg)) return;

    setActionLoading(true);
    try {
      await orderService.handleCustomerDecision(order.id, action, isAccept ? 'Khách hàng đồng ý mua phần được duyệt' : 'Khách hàng hủy toàn bộ đơn');
      success(isAccept ? 'Đã cập nhật đơn hàng! Bạn có thể tiến hành thanh toán.' : 'Đã hủy toàn bộ đơn hàng.');
      fetchOrderDetail();
    } catch (err) {
      error(err.message || 'Lỗi khi xử lý quyết định đơn hàng.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleOpenSubReview = (subOrder, action = 'APPROVE') => {
    setSubReviewModal({
      isOpen: true,
      subOrder,
      action,
      reason: '',
    });
  };

  const handleCloseSubReview = () => {
    setSubReviewModal({
      isOpen: false,
      subOrder: null,
      action: 'APPROVE',
      reason: '',
    });
  };

  const handleConfirmSubReview = async (e) => {
    if (e) e.preventDefault();
    const { subOrder, action, reason } = subReviewModal;
    if (!subOrder) return;
    if ((action === 'REJECT' || action === 'RETURN') && !reason.trim()) {
      error('Vui lòng nhập lý do từ chối / trả lại.');
      return;
    }

    setActionLoading(true);
    try {
      if (action === 'APPROVE') {
        await orderService.approveSubOrder(subOrder.id, reason.trim());
        success(`Đã phê duyệt đơn con #${subOrder.subOrderCode || subOrder.id}!`);
      } else if (action === 'REJECT') {
        await orderService.rejectSubOrder(subOrder.id, reason.trim());
        success(`Đã từ chối đơn con #${subOrder.subOrderCode || subOrder.id}!`);
      } else if (action === 'RETURN') {
        await orderService.returnSubOrder(subOrder.id, reason.trim());
        success(`Đã yêu cầu sửa đổi đơn con #${subOrder.subOrderCode || subOrder.id}!`);
      }
      handleCloseSubReview();
      fetchOrderDetail();
    } catch (err) {
      error(err.message || 'Lỗi khi xử lý đơn con.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleUpdateQuantity = async (productId, newQty) => {
    if (newQty < 1 || newQty > 99) return;
    setActionLoading(true);
    try {
      await orderService.updateOrderItemQuantity(order.id, productId, newQty);
      success('Đã cập nhật số lượng sản phẩm!');
      await fetchOrderDetail();
    } catch (err) {
      error(err.message || 'Lỗi khi cập nhật số lượng.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeleteItem = async (productId, productName) => {
    if (!window.confirm(`Bạn có chắc muốn xóa "${productName || 'sản phẩm'}" khỏi đơn hàng?`)) return;
    setActionLoading(true);
    try {
      await orderService.deleteOrderItem(order.id, productId);
      success('Đã xóa sản phẩm khỏi đơn hàng!');
      await fetchOrderDetail();
    } catch (err) {
      error(err.message || 'Lỗi khi xóa sản phẩm.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleOpenAddItem = async () => {
    setAddItemModal(prev => ({ ...prev, isOpen: true, loadingProducts: true }));
    try {
      const res = await productService.getProducts({ page: 0, size: 50, status: 'ACTIVE' });
      const items = res?.content || res?.data?.content || res?.data || (Array.isArray(res) ? res : []);
      setAddItemModal(prev => ({
        ...prev,
        products: items,
        productId: items.length > 0 ? items[0].id : '',
        quantity: 1,
        loadingProducts: false,
      }));
    } catch (err) {
      error('Không thể tải danh sách sản phẩm.');
      setAddItemModal(prev => ({ ...prev, loadingProducts: false }));
    }
  };

  const handleConfirmAddItem = async (e) => {
    if (e) e.preventDefault();
    if (!addItemModal.productId) {
      error('Vui lòng chọn sản phẩm.');
      return;
    }
    setActionLoading(true);
    try {
      await orderService.addOrderItem(order.id, {
        productId: Number(addItemModal.productId),
        quantity: Number(addItemModal.quantity),
      });
      success('Đã thêm sản phẩm vào đơn hàng!');
      setAddItemModal(prev => ({ ...prev, isOpen: false }));
      await fetchOrderDetail();
    } catch (err) {
      error(err.message || 'Lỗi khi thêm sản phẩm vào đơn hàng.');
    } finally {
      setActionLoading(false);
    }
  };

  const canReviewSub = (sub) => {
    if (!order || order.status === 'CANCELLED' || order.status === 'REJECTED' || order.status === 'COMPLETED' || order.status === 'PAID') return false;
    if (sub.status !== 'PENDING_APPROVAL' && sub.status !== 'PENDING_PO_REVIEW') return false;
    if (isAdmin()) return true;
    if (!isProductOwner()) return false;

    const currentUsername = user?.username ? user.username.toLowerCase() : '';
    const currentUserId = user?.id ? Number(user.id) : null;

    if (sub.managedByUsername && sub.managedByUsername.toLowerCase() === currentUsername) return true;
    if (sub.managedByUserId && currentUserId && Number(sub.managedByUserId) === currentUserId) return true;
    if (sub.orderItems && sub.orderItems.some(it =>
      (it.managedByUsername && it.managedByUsername.toLowerCase() === currentUsername) ||
      (it.managedByUserId && currentUserId && Number(it.managedByUserId) === currentUserId)
    )) return true;

    return false;
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
  const isActionRequired = order.status === 'ACTION_REQUIRED' || order.canMakeDecision;
  const isEditable = [
    ORDER_STATUS.DRAFT,
    ORDER_STATUS.PENDING_APPROVAL,
    ORDER_STATUS.PENDING_PO_REVIEW
  ].includes(order.status) && (isCustomer() || isAdmin());

  const canCancel = [
    ORDER_STATUS.DRAFT,
    ORDER_STATUS.PENDING_APPROVAL,
    ORDER_STATUS.PENDING_PO_REVIEW,
    ORDER_STATUS.PAYMENT_PENDING,
    ORDER_STATUS.RETURNED,
    ORDER_STATUS.PAID
  ].includes(order.status);
  const canSubmit = order.status === ORDER_STATUS.DRAFT || order.status === ORDER_STATUS.RETURNED;

  const displayedSubOrders = (!isAdmin() && isProductOwner() && user && order.subOrders)
    ? order.subOrders.filter(sub => {
        const u = (user.username || '').toLowerCase();
        const uid = user.id ? Number(user.id) : null;
        if (sub.managedByUsername && sub.managedByUsername.toLowerCase() === u) return true;
        if (sub.managedByUserId && uid && Number(sub.managedByUserId) === uid) return true;
        if (sub.orderItems && sub.orderItems.some(it =>
          (it.managedByUsername && it.managedByUsername.toLowerCase() === u) ||
          (it.managedByUserId && uid && Number(it.managedByUserId) === uid)
        )) return true;
        return false;
      })
    : (order.subOrders || []);

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
            <Link to={invoice?.id ? `/invoices/${invoice.id}` : `/invoices`} className="btn btn-sm btn-success">
              <Receipt size={16} /> {invoice?.invoiceNumber ? `Hóa đơn: ${invoice.invoiceNumber}` : 'Xem hóa đơn VAT'}
            </Link>
          )}
        </div>
      </div>

      <div className="page-header" style={{ marginBottom: '1.5rem' }}>
        <div>
          <h1 className="page-title">
            Đơn hàng #{order.id} {order.orderCode ? `(${order.orderCode})` : ''}
          </h1>
          <p className="page-subtitle">Ngày đặt: {formatDateTime(order.createdAt)}</p>
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          {isEditable && (
            <button
              onClick={handleOpenAddItem}
              disabled={actionLoading}
              className="btn btn-primary"
              style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
            >
              <Plus size={16} /> Thêm sản phẩm
            </button>
          )}

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
            isCustomer() ? (
              <button
                onClick={handlePayVnPay}
                disabled={actionLoading}
                className="btn btn-primary"
                style={{ background: 'linear-gradient(135deg, #0284c7, #0369a1)' }}
              >
                <CreditCard size={16} /> Thanh toán VNPay ngay ({formatCurrency(order.totalAmount)})
              </button>
            ) : (
              <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.4rem',
                fontSize: '0.85rem',
                color: '#b45309',
                fontWeight: 600,
                background: 'var(--color-warning-light)',
                padding: '0.5rem 0.85rem',
                borderRadius: 'var(--radius-md)'
              }}>
                <ShieldCheck size={16} /> Chờ khách hàng thanh toán (Admin/PO không thể tự thanh toán)
              </div>
            )
          )}

          {canCancel && !isActionRequired && (
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

      {/* Editable Order Notice Banner */}
      {isEditable && (
        <div style={{
          background: 'linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%)',
          border: '1.5px solid #93c5fd',
          borderRadius: 'var(--radius-lg)',
          padding: '1rem 1.25rem',
          marginBottom: '1.5rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '0.75rem',
          boxShadow: '0 2px 6px rgba(59, 130, 246, 0.08)'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div style={{
              background: '#3b82f6',
              color: '#ffffff',
              borderRadius: '50%',
              width: '36px',
              height: '36px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0
            }}>
              <Edit3 size={18} />
            </div>
            <div>
              <strong style={{ color: '#1e40af', fontSize: '0.95rem', display: 'block' }}>
                Đơn hàng đang ở trạng thái {ORDER_STATUS_LABELS[order.status] || order.status} (Cho phép chỉnh sửa)
              </strong>
              <span style={{ fontSize: '0.85rem', color: '#1e3a8a' }}>
                Bạn có thể tăng/giảm số lượng, xóa sản phẩm hoặc bấm "Thêm sản phẩm" để bổ sung món mới vào đơn hàng này.
              </span>
            </div>
          </div>
          <button
            onClick={handleOpenAddItem}
            disabled={actionLoading}
            className="btn btn-sm btn-primary"
            style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}
          >
            <Plus size={15} /> Thêm sản phẩm
          </button>
        </div>
      )}

      {/* Thông tin hoàn tiền khi đơn hàng đã hủy và có thông tin hoàn trả */}
      {order.status === ORDER_STATUS.CANCELLED && (payment?.refundAmount > 0 || payment?.status === 'REFUNDED' || payment?.refundTransactionNo) && (
        <div style={{
          background: 'linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%)',
          border: '1.5px solid #a7f3d0',
          borderRadius: 'var(--radius-lg)',
          padding: '1.5rem',
          marginBottom: '1.75rem',
          boxShadow: '0 4px 12px rgba(16, 185, 129, 0.08)',
        }}>
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '1rem', flexWrap: 'wrap' }}>
            <div style={{
              background: '#10b981',
              color: '#ffffff',
              borderRadius: '50%',
              width: '42px',
              height: '42px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
              boxShadow: '0 2px 6px rgba(16, 185, 129, 0.3)'
            }}>
              <CheckCircle2 size={24} />
            </div>
            <div style={{ flex: 1, minWidth: '280px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem', marginBottom: '0.5rem' }}>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: '#065f46', margin: 0 }}>
                  Thông tin hoàn tiền về tài khoản
                </h3>
                <Badge variant="success">
                  ĐÃ HOÀN TIỀN
                </Badge>
              </div>
              <p style={{ fontSize: '0.9rem', color: '#047857', marginBottom: '1rem', lineHeight: 1.5 }}>
                Đơn hàng đã được hủy và giao dịch hoàn trả <strong>{formatCurrency(payment?.refundAmount || order.totalAmount)}</strong> đã được thực hiện thành công về tài khoản thanh toán của quý khách.
              </p>

              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: '0.85rem',
                background: '#ffffff',
                padding: '1rem 1.25rem',
                borderRadius: 'var(--radius-md)',
                border: '1px solid #d1fae5'
              }}>
                <div>
                  <span style={{ fontSize: '0.75rem', color: '#6b7280', textTransform: 'uppercase', fontWeight: 700, letterSpacing: '0.5px' }}>Số tiền hoàn lại</span>
                  <div style={{ fontSize: '1.2rem', fontWeight: 800, color: '#059669', marginTop: '2px' }}>
                    {formatCurrency(payment?.refundAmount || order.totalAmount)}
                  </div>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: '#6b7280', textTransform: 'uppercase', fontWeight: 700, letterSpacing: '0.5px' }}>Mã giao dịch hoàn</span>
                  <div style={{ fontSize: '0.95rem', fontWeight: 700, color: '#0284c7', marginTop: '2px', wordBreak: 'break-all' }}>
                    {payment?.refundTransactionNo || payment?.transactionNo || `REF_${order.id}`}
                  </div>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: '#6b7280', textTransform: 'uppercase', fontWeight: 700, letterSpacing: '0.5px' }}>Thời gian hoàn tiền</span>
                  <div style={{ fontSize: '0.9rem', fontWeight: 600, color: '#374151', marginTop: '2px' }}>
                    {formatDateTime(payment?.refundedAt || order.statusChangedAt || order.updatedAt)}
                  </div>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: '#6b7280', textTransform: 'uppercase', fontWeight: 700, letterSpacing: '0.5px' }}>Lý do hủy đơn</span>
                  <div style={{ fontSize: '0.9rem', fontWeight: 500, color: '#374151', marginTop: '2px' }}>
                    {payment?.refundReason || order.cancelReason || 'Khách hàng yêu cầu hủy đơn'}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Customer Decision Banner when ACTION_REQUIRED */}
      {isActionRequired && (
        <div style={{
          background: 'linear-gradient(135deg, #fef2f2, #fff1f2)',
          border: '1.5px solid #fecdd3',
          padding: '1.5rem',
          borderRadius: 'var(--radius-lg)',
          marginBottom: '1.75rem',
          boxShadow: 'var(--shadow-sm)',
        }}>
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.875rem' }}>
            <AlertTriangle size={26} color="#e11d48" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div style={{ flex: 1 }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 800, color: '#9f1239', marginBottom: '0.35rem' }}>
                Xác nhận từ bạn: Một số sản phẩm bị Product Owner từ chối
              </h3>
              <p style={{ fontSize: '0.9rem', color: '#881337', marginBottom: '1rem', lineHeight: 1.5 }}>
                Đơn hàng của bạn chứa các mặt hàng thuộc nhiều ngành hàng khác nhau. Các Product Owner đã hoàn tất duyệt: một số sản phẩm đã được chấp thuận và một số bị từ chối. Vui lòng chọn cách xử lý:
              </p>

              <div style={{ display: 'flex', gap: '0.875rem', flexWrap: 'wrap' }}>
                <button
                  onClick={() => handleDecision('ACCEPT_PARTIAL')}
                  disabled={actionLoading}
                  className="btn btn-primary"
                  style={{ background: '#059669', borderColor: '#059669' }}
                >
                  ✓ Tiếp tục mua các món được duyệt (Loại bỏ món bị từ chối)
                </button>
                <button
                  onClick={() => handleDecision('CANCEL_ALL')}
                  disabled={actionLoading}
                  className="btn btn-danger"
                >
                  ✕ Hủy toàn bộ đơn hàng
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* PO Review Feedback Box if Returned or Rejected */}
      {(order.returnedReason || order.rejectedReason) && !isActionRequired && (
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
        {/* Left: Product Items & Sub-Orders */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {displayedSubOrders && displayedSubOrders.length > 0 ? (
            displayedSubOrders.map((sub, idx) => {
              const poCanReview = canReviewSub(sub);
              return (
                <div key={sub.id} className="card" style={{ border: sub.status === 'REJECTED' ? '1.5px solid #fca5a5' : sub.status === 'APPROVED' ? '1.5px solid #86efac' : '1px solid var(--border-color)' }}>
                  <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#f8fafc', padding: '0.85rem 1.25rem' }}>
                    <div>
                      <h4 style={{ fontSize: '0.95rem', fontWeight: 800 }}>
                        Đơn con #{idx + 1}: {sub.subOrderCode || `SubOrder #${sub.id}`}
                      </h4>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        PO quản lý: <strong style={{ color: sub.managedByUsername === user?.username ? '#16a34a' : 'inherit' }}>
                          {sub.managedByUsername || 'Chung / Admin'}
                          {sub.managedByUsername === user?.username && ' (Bạn)'}
                        </strong>
                      </span>
                    </div>
                    <Badge variant={ORDER_STATUS_BADGE[sub.status] || 'neutral'}>
                      {ORDER_STATUS_LABELS[sub.status] || sub.status}
                    </Badge>
                  </div>

                  {sub.rejectedReason && (
                    <div style={{ padding: '0.65rem 1.25rem', background: '#fef2f2', color: '#991b1b', fontSize: '0.85rem', borderBottom: '1px solid #fee2e2' }}>
                      <strong>Lý do từ chối:</strong> {sub.rejectedReason}
                    </div>
                  )}

                  {sub.returnedReason && (
                    <div style={{ padding: '0.65rem 1.25rem', background: '#fffbeb', color: '#b45309', fontSize: '0.85rem', borderBottom: '1px solid #fef3c7' }}>
                      <strong>Lý do yêu cầu sửa đổi:</strong> {sub.returnedReason}
                    </div>
                  )}

                  {sub.reviewNotes && sub.status === 'APPROVED' && (
                    <div style={{ padding: '0.65rem 1.25rem', background: '#f0fdf4', color: '#166534', fontSize: '0.85rem', borderBottom: '1px solid #dcfce7' }}>
                      <strong>Ghi chú duyệt:</strong> {sub.reviewNotes}
                    </div>
                  )}

                  <div style={{ padding: '1rem 1.25rem', display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
                    {sub.orderItems?.map((item) => {
                      const pid = item.productId || item.product?.id;
                      return (
                        <div
                          key={item.id || pid}
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            paddingBottom: '0.65rem',
                            borderBottom: '1px solid #f1f5f9',
                            flexWrap: 'wrap',
                            gap: '0.5rem',
                          }}
                        >
                          <div style={{ flex: '1 1 200px' }}>
                            <h5 style={{ fontSize: '0.9rem', fontWeight: 700, margin: 0 }}>
                              {item.productName || item.product?.name || `Sản phẩm #${pid}`}
                            </h5>
                            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                              Đơn giá: {formatCurrency(item.unitPrice)}
                            </span>
                          </div>

                          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                            {isEditable ? (
                              <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', background: '#f1f5f9', padding: '0.2rem 0.4rem', borderRadius: 'var(--radius-md)' }}>
                                <button
                                  type="button"
                                  onClick={() => handleUpdateQuantity(pid, item.quantity - 1)}
                                  disabled={actionLoading || item.quantity <= 1}
                                  style={{
                                    border: 'none',
                                    background: item.quantity <= 1 ? '#e2e8f0' : '#ffffff',
                                    color: item.quantity <= 1 ? '#94a3b8' : 'var(--text-main)',
                                    borderRadius: '4px',
                                    width: '26px',
                                    height: '26px',
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    cursor: item.quantity <= 1 ? 'not-allowed' : 'pointer',
                                    boxShadow: '0 1px 2px rgba(0,0,0,0.05)'
                                  }}
                                  title="Giảm số lượng"
                                >
                                  <Minus size={13} />
                                </button>
                                <span style={{ minWidth: '24px', textAlign: 'center', fontWeight: 700, fontSize: '0.85rem' }}>
                                  {item.quantity}
                                </span>
                                <button
                                  type="button"
                                  onClick={() => handleUpdateQuantity(pid, item.quantity + 1)}
                                  disabled={actionLoading || item.quantity >= 99}
                                  style={{
                                    border: 'none',
                                    background: item.quantity >= 99 ? '#e2e8f0' : '#ffffff',
                                    color: item.quantity >= 99 ? '#94a3b8' : 'var(--text-main)',
                                    borderRadius: '4px',
                                    width: '26px',
                                    height: '26px',
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    cursor: item.quantity >= 99 ? 'not-allowed' : 'pointer',
                                    boxShadow: '0 1px 2px rgba(0,0,0,0.05)'
                                  }}
                                  title="Tăng số lượng"
                                >
                                  <Plus size={13} />
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleDeleteItem(pid, item.productName || item.product?.name)}
                                  disabled={actionLoading}
                                  style={{
                                    border: 'none',
                                    background: '#fee2e2',
                                    color: '#dc2626',
                                    borderRadius: '4px',
                                    width: '26px',
                                    height: '26px',
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    cursor: 'pointer',
                                    marginLeft: '0.25rem'
                                  }}
                                  title="Xóa khỏi đơn hàng"
                                >
                                  <Trash2 size={13} />
                                </button>
                              </div>
                            ) : (
                              <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                                × {item.quantity}
                              </span>
                            )}

                            <span style={{ fontWeight: 700, color: 'var(--text-main)', minWidth: '90px', textAlign: 'right' }}>
                              {formatCurrency(item.subTotal || item.subtotal || (item.unitPrice * item.quantity))}
                            </span>
                          </div>
                        </div>
                      );
                    })}
                    <div style={{ display: 'flex', justifyContent: 'space-between', paddingTop: '0.5rem', fontWeight: 700, fontSize: '0.9rem' }}>
                      <span>Tổng đơn con:</span>
                      <span style={{ color: 'var(--color-primary)' }}>{formatCurrency(sub.totalAmount)}</span>
                    </div>
                  </div>

                  {/* Actions for PO or Admin on this specific Sub-Order */}
                  {poCanReview && (
                    <div style={{
                      padding: '0.75rem 1.25rem',
                      background: '#f8fafc',
                      borderTop: '1px solid var(--border-color)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      flexWrap: 'wrap',
                      gap: '0.5rem'
                    }}>
                      <span style={{ fontSize: '0.825rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                        Thao tác đơn con:
                      </span>
                      <div style={{ display: 'flex', gap: '0.5rem' }}>
                        <button
                          type="button"
                          onClick={() => handleOpenSubReview(sub, 'APPROVE')}
                          disabled={actionLoading}
                          className="btn btn-sm btn-success"
                        >
                          <CheckCircle2 size={14} /> Duyệt đơn con
                        </button>
                        <button
                          type="button"
                          onClick={() => handleOpenSubReview(sub, 'RETURN')}
                          disabled={actionLoading}
                          className="btn btn-sm btn-warning"
                        >
                          <RefreshCw size={14} /> Yêu cầu sửa
                        </button>
                        <button
                          type="button"
                          onClick={() => handleOpenSubReview(sub, 'REJECT')}
                          disabled={actionLoading}
                          className="btn btn-sm btn-danger"
                        >
                          <XCircle size={14} /> Từ chối
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              );
            })
          ) : (
            <div className="card">
              <div className="card-header">
                <h3 className="card-title">
                  <FileText size={20} color="var(--color-primary)" /> Danh sách sản phẩm
                </h3>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', padding: '1.25rem' }}>
                {order.orderItems?.map((item) => {
                  const pid = item.productId || item.product?.id;
                  return (
                    <div
                      key={item.id || pid}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        paddingBottom: '0.75rem',
                        borderBottom: '1px solid var(--border-color)',
                        flexWrap: 'wrap',
                        gap: '0.5rem',
                      }}
                    >
                      <div style={{ flex: '1 1 200px' }}>
                        <h4 style={{ fontSize: '0.95rem', fontWeight: 700, margin: 0 }}>
                          {item.product?.name || item.productName || `Sản phẩm #${pid || ''}`}
                        </h4>
                        <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                          Đơn giá: {formatCurrency(item.unitPrice)}
                        </span>
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                        {isEditable ? (
                          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', background: '#f1f5f9', padding: '0.2rem 0.4rem', borderRadius: 'var(--radius-md)' }}>
                            <button
                              type="button"
                              onClick={() => handleUpdateQuantity(pid, item.quantity - 1)}
                              disabled={actionLoading || item.quantity <= 1}
                              style={{
                                border: 'none',
                                background: item.quantity <= 1 ? '#e2e8f0' : '#ffffff',
                                color: item.quantity <= 1 ? '#94a3b8' : 'var(--text-main)',
                                borderRadius: '4px',
                                width: '26px',
                                height: '26px',
                                display: 'inline-flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                cursor: item.quantity <= 1 ? 'not-allowed' : 'pointer',
                                boxShadow: '0 1px 2px rgba(0,0,0,0.05)'
                              }}
                              title="Giảm số lượng"
                            >
                              <Minus size={13} />
                            </button>
                            <span style={{ minWidth: '24px', textAlign: 'center', fontWeight: 700, fontSize: '0.85rem' }}>
                              {item.quantity}
                            </span>
                            <button
                              type="button"
                              onClick={() => handleUpdateQuantity(pid, item.quantity + 1)}
                              disabled={actionLoading || item.quantity >= 99}
                              style={{
                                border: 'none',
                                background: item.quantity >= 99 ? '#e2e8f0' : '#ffffff',
                                color: item.quantity >= 99 ? '#94a3b8' : 'var(--text-main)',
                                borderRadius: '4px',
                                width: '26px',
                                height: '26px',
                                display: 'inline-flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                cursor: item.quantity >= 99 ? 'not-allowed' : 'pointer',
                                boxShadow: '0 1px 2px rgba(0,0,0,0.05)'
                              }}
                              title="Tăng số lượng"
                            >
                              <Plus size={13} />
                            </button>
                            <button
                              type="button"
                              onClick={() => handleDeleteItem(pid, item.product?.name || item.productName)}
                              disabled={actionLoading}
                              style={{
                                border: 'none',
                                background: '#fee2e2',
                                color: '#dc2626',
                                borderRadius: '4px',
                                width: '26px',
                                height: '26px',
                                display: 'inline-flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                cursor: 'pointer',
                                marginLeft: '0.25rem'
                              }}
                              title="Xóa khỏi đơn hàng"
                            >
                              <Trash2 size={13} />
                            </button>
                          </div>
                        ) : (
                          <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                            × {item.quantity}
                          </span>
                        )}

                        <span style={{ fontWeight: 800, color: 'var(--text-main)', minWidth: '90px', textAlign: 'right' }}>
                          {formatCurrency(item.subTotal || item.subtotal || (item.unitPrice * item.quantity))}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Master Order Total Summary */}
          <div className="card" style={{ padding: '1.25rem', background: '#f8fafc' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '1.1rem', fontWeight: 700 }}>Tổng thanh toán Master Order:</span>
              <span style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--color-primary)' }}>
                {formatCurrency(order.totalAmount)}
              </span>
            </div>
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

      {/* Modal for Sub-Order review (Approve / Reject / Return) */}
      <Modal
        isOpen={subReviewModal.isOpen}
        onClose={handleCloseSubReview}
        title={`${
          subReviewModal.action === 'APPROVE'
            ? 'Phê duyệt'
            : subReviewModal.action === 'REJECT'
            ? 'Từ chối'
            : 'Yêu cầu sửa đổi'
        } Đơn con #${subReviewModal.subOrder?.subOrderCode || subReviewModal.subOrder?.id}`}
        maxWidth="500px"
        footer={
          <>
            <button type="button" onClick={handleCloseSubReview} className="btn btn-secondary">
              Hủy
            </button>
            <button
              type="button"
              onClick={handleConfirmSubReview}
              disabled={actionLoading || ((subReviewModal.action === 'REJECT' || subReviewModal.action === 'RETURN') && !subReviewModal.reason.trim())}
              className={`btn ${
                subReviewModal.action === 'APPROVE'
                  ? 'btn-success'
                  : subReviewModal.action === 'REJECT'
                  ? 'btn-danger'
                  : 'btn-warning'
              }`}
            >
              {actionLoading ? 'Đang xử lý...' : 'Xác nhận'}
            </button>
          </>
        }
      >
        <form onSubmit={handleConfirmSubReview}>
          <p style={{ fontSize: '0.9rem', marginBottom: '1rem', color: 'var(--text-muted)' }}>
            Đơn con gồm <strong>{subReviewModal.subOrder?.orderItems?.length || 0} sản phẩm</strong>, tổng tiền{' '}
            <strong style={{ color: 'var(--color-primary)' }}>{formatCurrency(subReviewModal.subOrder?.totalAmount)}</strong>.
          </p>

          <div className="form-group">
            <label className="form-label" style={{ fontWeight: 600 }}>
              {subReviewModal.action === 'APPROVE'
                ? 'Ghi chú phê duyệt (Tùy chọn):'
                : 'Lý do từ chối / yêu cầu sửa đổi (Bắt buộc):'}
            </label>
            <textarea
              rows={3}
              required={subReviewModal.action === 'REJECT' || subReviewModal.action === 'RETURN'}
              className="form-control"
              placeholder={
                subReviewModal.action === 'APPROVE'
                  ? 'Ví dụ: Đã kiểm tra số lượng và phê duyệt.'
                  : 'Ví dụ: Hết hàng trong kho hoặc cần điều chỉnh số lượng...'
              }
              value={subReviewModal.reason}
              onChange={(e) => setSubReviewModal({ ...subReviewModal, reason: e.target.value })}
            />
          </div>
        </form>
      </Modal>

      {/* Modal for adding product to editable order */}
      <Modal
        isOpen={addItemModal.isOpen}
        onClose={() => setAddItemModal(prev => ({ ...prev, isOpen: false }))}
        title="Thêm sản phẩm vào đơn hàng"
        maxWidth="520px"
        footer={
          <>
            <button
              type="button"
              onClick={() => setAddItemModal(prev => ({ ...prev, isOpen: false }))}
              className="btn btn-secondary"
            >
              Hủy
            </button>
            <button
              type="button"
              onClick={handleConfirmAddItem}
              disabled={actionLoading || !addItemModal.productId || addItemModal.loadingProducts}
              className="btn btn-primary"
            >
              {actionLoading ? 'Đang thêm...' : 'Thêm vào đơn hàng'}
            </button>
          </>
        }
      >
        <form onSubmit={handleConfirmAddItem}>
          {addItemModal.loadingProducts ? (
            <div style={{ textAlign: 'center', padding: '2rem 1rem' }}>
              <LoadingSpinner text="Đang tải danh sách sản phẩm..." />
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div className="form-group">
                <label className="form-label" style={{ fontWeight: 600 }}>
                  Chọn sản phẩm:
                </label>
                <select
                  className="form-control"
                  value={addItemModal.productId}
                  onChange={(e) => setAddItemModal(prev => ({ ...prev, productId: e.target.value }))}
                  required
                >
                  {addItemModal.products?.map((p) => (
                    <option key={p.id} value={p.id} disabled={p.stock <= 0}>
                      {p.name} — {formatCurrency(p.price)} (Còn: {p.stock})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" style={{ fontWeight: 600 }}>
                  Số lượng (1 - 99):
                </label>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <button
                    type="button"
                    onClick={() => setAddItemModal(prev => ({ ...prev, quantity: Math.max(1, prev.quantity - 1) }))}
                    disabled={addItemModal.quantity <= 1}
                    className="btn btn-outline"
                    style={{ width: '36px', height: '36px', padding: 0, display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}
                  >
                    <Minus size={16} />
                  </button>
                  <input
                    type="number"
                    min="1"
                    max="99"
                    required
                    className="form-control"
                    style={{ width: '80px', textAlign: 'center', fontWeight: 700 }}
                    value={addItemModal.quantity}
                    onChange={(e) => {
                      const val = parseInt(e.target.value, 10);
                      setAddItemModal(prev => ({ ...prev, quantity: isNaN(val) ? 1 : Math.min(99, Math.max(1, val)) }));
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => setAddItemModal(prev => ({ ...prev, quantity: Math.min(99, prev.quantity + 1) }))}
                    disabled={addItemModal.quantity >= 99}
                    className="btn btn-outline"
                    style={{ width: '36px', height: '36px', padding: 0, display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}
                  >
                    <Plus size={16} />
                  </button>
                </div>
              </div>

              {addItemModal.productId && (() => {
                const selected = addItemModal.products?.find(p => String(p.id) === String(addItemModal.productId));
                if (!selected) return null;
                const estimatedTotal = (selected.price || 0) * (addItemModal.quantity || 1);
                return (
                  <div style={{
                    background: '#f8fafc',
                    padding: '0.85rem 1rem',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--border-color)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}>
                    <span style={{ fontSize: '0.9rem', color: 'var(--text-muted)' }}>Thành tiền ước tính:</span>
                    <strong style={{ fontSize: '1.1rem', color: 'var(--color-primary)' }}>
                      {formatCurrency(estimatedTotal)}
                    </strong>
                  </div>
                );
              })()}
            </div>
          )}
        </form>
      </Modal>
    </div>
  );
};

export default OrderDetailPage;
