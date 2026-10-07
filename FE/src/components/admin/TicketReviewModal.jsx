import React, { useState, useEffect } from 'react';
import Modal from '../common/Modal';
import LoadingSpinner from '../common/LoadingSpinner';
import Badge from '../common/Badge';
import ticketService from '../../services/ticket.service';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import { TICKET_STATUS_LABELS } from '../../utils/constants';
import { useAuth } from '../../hooks/useAuth';
import { CheckCircle2, XCircle, RefreshCw, ShoppingCart, User, Calendar, FileText, Lock, ShieldAlert, Tag } from 'lucide-react';

export const TicketReviewModal = ({
  isOpen,
  onClose,
  ticket,
  onApprove,
  onReject,
  onReturn,
  loading = false,
}) => {
  const { user, isAdmin, isProductOwner } = useAuth();
  const [ticketDetail, setTicketDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [actionType, setActionType] = useState('APPROVE'); // 'APPROVE' | 'REJECT' | 'RETURN'
  const [noteOrReason, setNoteOrReason] = useState('');

  useEffect(() => {
    if (isOpen && ticket?.id) {
      setNoteOrReason('');
      setActionType('APPROVE');
      setDetailLoading(true);
      ticketService
        .getTicketById(ticket.id)
        .then((data) => setTicketDetail(data))
        .catch(() => setTicketDetail(ticket))
        .finally(() => setDetailLoading(false));
    } else {
      setTicketDetail(null);
    }
  }, [isOpen, ticket]);

  if (!ticket) return null;

  const current = ticketDetail || ticket;
  const isPending = current.status === 'PENDING_APPROVAL';
  const orderItems = current.orderItems || current.subOrderDetails?.orderItems || current.orderDetails?.orderItems || current.orderDetails?.items || [];

  const canReview = (() => {
    if (isAdmin()) return true;
    if (!isProductOwner()) return false;

    const currentUsername = user?.username ? user.username.toLowerCase() : '';
    const currentUserId = user?.id ? Number(user.id) : null;

    // Check if PO matches reviewer or managedBy of the ticket/subOrder
    if (current.reviewerUsername && current.reviewerUsername.toLowerCase() === currentUsername) return true;
    if (current.managedByUsername && current.managedByUsername.toLowerCase() === currentUsername) return true;
    if (current.subOrderDetails?.managedByUsername && current.subOrderDetails.managedByUsername.toLowerCase() === currentUsername) return true;

    if (current.managedByUserId && currentUserId && Number(current.managedByUserId) === currentUserId) return true;
    if (current.subOrderDetails?.managedByUserId && currentUserId && Number(current.subOrderDetails.managedByUserId) === currentUserId) return true;

    // Check if any product/item in this sub-order belongs to PO
    if (orderItems.length > 0) {
      return orderItems.some((item) => {
        if (item.managedByUsername && currentUsername) {
          return item.managedByUsername.toLowerCase() === currentUsername;
        }
        if (item.managedByUserId && currentUserId) {
          return Number(item.managedByUserId) === currentUserId;
        }
        return false;
      });
    }

    return true;
  })();

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!isPending) return;
    if (actionType === 'APPROVE') {
      onApprove(current.id, noteOrReason);
    } else if (actionType === 'REJECT') {
      onReject(current.id, noteOrReason);
    } else if (actionType === 'RETURN') {
      onReturn(current.id, noteOrReason);
    }
  };

  const isRejectOrReturn = actionType === 'REJECT' || actionType === 'RETURN';

  const getStatusBadgeVariant = (status) => {
    switch (status) {
      case 'APPROVED': return 'success';
      case 'REJECTED': return 'danger';
      case 'RETURNED': return 'info';
      case 'CANCELLED': return 'danger';
      default: return 'warning';
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Xét duyệt Phiếu #${current.ticketCode || current.id} ${current.subOrderCode ? `(Đơn con ${current.subOrderCode})` : ''} ${!canReview && isPending ? '(Chỉ xem)' : ''}`}
      maxWidth="760px"
      footer={
        <>
          <button type="button" onClick={onClose} className="btn btn-secondary">
            {isPending && canReview ? 'Hủy' : 'Đóng'}
          </button>
          {isPending && canReview && (
            <button
              type="submit"
              form="ticket-review-form"
              disabled={loading || (isRejectOrReturn && !noteOrReason.trim())}
              className={`btn ${
                actionType === 'APPROVE'
                  ? 'btn-success'
                  : actionType === 'REJECT'
                  ? 'btn-danger'
                  : 'btn-warning'
              }`}
            >
              {loading
                ? 'Đang xử lý...'
                : `Xác nhận ${
                    actionType === 'APPROVE'
                      ? 'Phê duyệt'
                      : actionType === 'REJECT'
                      ? 'Từ chối'
                      : 'Yêu cầu sửa đổi'
                  }`}
            </button>
          )}
        </>
      }
    >
      {detailLoading ? (
        <LoadingSpinner text="Đang tải chi tiết phiếu mua hàng..." />
      ) : (
        <form id="ticket-review-form" onSubmit={handleSubmit}>
          {/* Warning Banner when PO cannot review this ticket */}
          {isPending && !canReview && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                padding: '0.85rem 1rem',
                backgroundColor: '#fef2f2',
                border: '1px solid #fecaca',
                borderRadius: 'var(--radius-md)',
                color: '#991b1b',
                fontSize: '0.85rem',
                marginBottom: '1.25rem',
              }}
            >
              <ShieldAlert size={20} style={{ flexShrink: 0, color: '#dc2626' }} />
              <div>
                <strong>Không có quyền xét duyệt:</strong> Phiếu mua hàng này thuộc đơn con do PO khác quản lý. Bạn chỉ có quyền xem chi tiết thông tin phiếu, không thể phê duyệt, từ chối hay yêu cầu sửa đổi.
              </div>
            </div>
          )}

          {/* Ticket Header Overview */}
          <div
            style={{
              background: '#f8fafc',
              padding: '1.25rem',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-color)',
              marginBottom: '1.25rem',
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '1rem',
              fontSize: '0.875rem',
            }}
          >
            <div>
              <span style={{ color: 'var(--text-muted)' }}>Mã phiếu PO:</span>
              <div style={{ fontWeight: 800, color: 'var(--color-primary)', fontSize: '1rem' }}>
                {current.ticketCode}
              </div>
            </div>
            {current.subOrderCode && (
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Đơn con (Sub-Order):</span>
                <div style={{ fontWeight: 700, color: '#0284c7' }}>{current.subOrderCode}</div>
              </div>
            )}
            <div>
              <span style={{ color: 'var(--text-muted)' }}>Master Order liên kết:</span>
              <div style={{ fontWeight: 700 }}>#{current.orderId}</div>
            </div>
            <div>
              <span style={{ color: 'var(--text-muted)' }}>PO phụ trách:</span>
              <div style={{ fontWeight: 700, color: current.managedByUsername === user?.username ? '#16a34a' : 'inherit' }}>
                {current.managedByUsername || current.reviewerUsername || 'Chưa phân công'}
                {current.managedByUsername === user?.username && ' (Bạn)'}
              </div>
            </div>
            <div>
              <span style={{ color: 'var(--text-muted)' }}>Khách hàng:</span>
              <div style={{ fontWeight: 700 }}>
                {current.customerName || `Customer #${current.customerId}`}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                {current.customerPhone || current.customerEmail}
              </div>
            </div>
            <div>
              <span style={{ color: 'var(--text-muted)' }}>Trạng thái:</span>
              <div>
                <Badge variant={getStatusBadgeVariant(current.status)}>
                  {TICKET_STATUS_LABELS[current.status] || current.status}
                </Badge>
              </div>
            </div>
            <div>
              <span style={{ color: 'var(--text-muted)' }}>Ngày tạo:</span>
              <div style={{ fontWeight: 600 }}>{formatDateTime(current.createdAt)}</div>
            </div>
            <div>
              <span style={{ color: 'var(--text-muted)' }}>Tổng thanh toán đơn con:</span>
              <div style={{ fontWeight: 800, color: 'var(--color-primary)', fontSize: '1.1rem' }}>
                {formatCurrency(current.totalAmount)}
              </div>
            </div>
          </div>

          {/* Order Items Table */}
          {orderItems.length > 0 && (
            <div style={{ marginBottom: '1.25rem' }}>
              <h4 style={{ fontSize: '0.95rem', fontWeight: 700, marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <ShoppingCart size={16} /> Danh sách sản phẩm trong đơn hàng
              </h4>
              <div className="table-responsive" style={{ border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)' }}>
                <table className="custom-table" style={{ fontSize: '0.85rem' }}>
                  <thead>
                    <tr>
                      <th>Sản phẩm</th>
                      <th>Loại hàng & PO</th>
                      <th style={{ textAlign: 'center' }}>Số lượng</th>
                      <th style={{ textAlign: 'right' }}>Đơn giá</th>
                      <th style={{ textAlign: 'right' }}>Thành tiền</th>
                    </tr>
                  </thead>
                  <tbody>
                    {orderItems.map((item, idx) => (
                      <tr key={idx}>
                        <td><strong>{item.productName || `Product #${item.productId}`}</strong></td>
                        <td>
                          {item.categoryName || item.categoryCode ? (
                            <div style={{ fontSize: '0.75rem' }}>
                              <span style={{ fontWeight: 600, color: '#334155' }}>
                                [{item.categoryCode || 'CAT'}] {item.categoryName}
                              </span>
                              <div style={{ color: item.managedByUsername === user?.username ? '#16a34a' : 'var(--text-muted)', fontWeight: item.managedByUsername === user?.username ? 600 : 'normal' }}>
                                PO: {item.managedByUsername || 'Chưa phân công'} {item.managedByUsername === user?.username && '(Bạn)'}
                              </div>
                            </div>
                          ) : (
                            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>—</span>
                          )}
                        </td>
                        <td style={{ textAlign: 'center' }}>{item.quantity}</td>
                        <td style={{ textAlign: 'right' }}>{formatCurrency(item.unitPrice)}</td>
                        <td style={{ textAlign: 'right' }}><strong>{formatCurrency(item.subTotal || item.subtotal || item.totalPrice || item.quantity * item.unitPrice)}</strong></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Past Review Information if already reviewed */}
          {!isPending && (
            <div
              style={{
                background: current.status === 'APPROVED' ? '#ecfdf5' : '#fef2f2',
                border: `1px solid ${current.status === 'APPROVED' ? '#a7f3d0' : '#fecaca'}`,
                padding: '1rem',
                borderRadius: 'var(--radius-md)',
                marginBottom: '1rem',
                fontSize: '0.875rem',
              }}
            >
              <div style={{ fontWeight: 700, marginBottom: '0.35rem', color: current.status === 'APPROVED' ? '#065f46' : '#991b1b' }}>
                Thông tin kết quả xét duyệt:
              </div>
              <div><strong>Người duyệt:</strong> {current.reviewerUsername || 'Product Owner'}</div>
              {current.reviewedAt && <div><strong>Thời gian:</strong> {formatDateTime(current.reviewedAt)}</div>}
              {current.reviewNotes && <div><strong>Ghi chú duyệt:</strong> {current.reviewNotes}</div>}
              {current.rejectionReason && <div><strong>Lý do từ chối:</strong> {current.rejectionReason}</div>}
              {current.returnedReason && <div><strong>Lý do trả lại:</strong> {current.returnedReason}</div>}
            </div>
          )}

          {/* Action Selection for Pending Tickets only if authorized */}
          {isPending && canReview && (
            <>
              <div className="form-group">
                <label className="form-label" style={{ fontWeight: 700 }}>
                  Quyết định của Product Owner / Admin:
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem' }}>
                  <button
                    type="button"
                    onClick={() => setActionType('APPROVE')}
                    className={`btn ${actionType === 'APPROVE' ? 'btn-success' : 'btn-outline'}`}
                    style={{ padding: '0.75rem' }}
                  >
                    <CheckCircle2 size={18} /> Phê duyệt (Approve)
                  </button>

                  <button
                    type="button"
                    onClick={() => setActionType('RETURN')}
                    className={`btn ${actionType === 'RETURN' ? 'btn-warning' : 'btn-outline'}`}
                    style={{ padding: '0.75rem' }}
                  >
                    <RefreshCw size={18} /> Trả lại (Return)
                  </button>

                  <button
                    type="button"
                    onClick={() => setActionType('REJECT')}
                    className={`btn ${actionType === 'REJECT' ? 'btn-danger' : 'btn-outline'}`}
                    style={{ padding: '0.75rem' }}
                  >
                    <XCircle size={18} /> Từ chối (Reject)
                  </button>
                </div>
              </div>

              {/* Note / Reason Field */}
              <div className="form-group">
                <label className="form-label">
                  {actionType === 'APPROVE'
                    ? 'Ghi chú phê duyệt (Tùy chọn)'
                    : 'Lý do từ chối / phản hồi sửa đổi *'}
                </label>
                <textarea
                  required={isRejectOrReturn}
                  placeholder={
                    actionType === 'APPROVE'
                      ? 'Ví dụ: Đã duyệt phiếu mua hàng theo hạn mức ngân sách.'
                      : 'Ví dụ: Sản phẩm trong đơn hàng đang tạm ngưng kinh doanh hoặc cần giảm số lượng...'
                  }
                  value={noteOrReason}
                  onChange={(e) => setNoteOrReason(e.target.value)}
                  className="form-control"
                  rows={3}
                />
              </div>
            </>
          )}
        </form>
      )}
    </Modal>
  );
};

export default TicketReviewModal;
