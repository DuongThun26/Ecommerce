import React, { useState } from 'react';
import Modal from '../common/Modal';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import { CheckCircle2, XCircle, RefreshCw, AlertCircle } from 'lucide-react';

export const TicketReviewModal = ({
  isOpen,
  onClose,
  ticket,
  onApprove,
  onReject,
  onReturn,
  loading = false,
}) => {
  const [actionType, setActionType] = useState('APPROVE'); // 'APPROVE' | 'REJECT' | 'RETURN'
  const [noteOrReason, setNoteOrReason] = useState('');

  if (!ticket) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (actionType === 'APPROVE') {
      onApprove(ticket.id, noteOrReason);
    } else if (actionType === 'REJECT') {
      onReject(ticket.id, noteOrReason);
    } else if (actionType === 'RETURN') {
      onReturn(ticket.id, noteOrReason);
    }
  };

  const isRejectOrReturn = actionType === 'REJECT' || actionType === 'RETURN';

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Xét duyệt Phiếu Mua Hàng #${ticket.ticketCode}`}
      maxWidth="680px"
      footer={
        <>
          <button type="button" onClick={onClose} className="btn btn-secondary">
            Đóng
          </button>
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
            {loading ? 'Đang xử lý...' : `Xác nhận ${actionType === 'APPROVE' ? 'Phê duyệt' : actionType === 'REJECT' ? 'Từ chối' : 'Yêu cầu sửa đổi'}`}
          </button>
        </>
      }
    >
      <form id="ticket-review-form" onSubmit={handleSubmit}>
        {/* Ticket & Order Summary */}
        <div style={{
          background: '#f8fafc',
          padding: '1rem',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border-color)',
          marginBottom: '1.25rem',
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '0.75rem',
          fontSize: '0.9rem',
        }}>
          <div><strong>Mã phiếu:</strong> {ticket.ticketCode}</div>
          <div><strong>Đơn hàng:</strong> #{ticket.orderId}</div>
          <div><strong>Khách hàng:</strong> {ticket.customerName} ({ticket.customerPhone})</div>
          <div><strong>Tổng giá trị:</strong> <span style={{ color: 'var(--color-primary)', fontWeight: 700 }}>{formatCurrency(ticket.totalAmount)}</span></div>
          <div><strong>Ngày gửi duyệt:</strong> {formatDateTime(ticket.createdAt)}</div>
          <div><strong>Trạng thái:</strong> {ticket.status}</div>
        </div>

        {/* Action Selection */}
        <div className="form-group">
          <label className="form-label">Quyết định của Product Owner:</label>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem' }}>
            <button
              type="button"
              onClick={() => setActionType('APPROVE')}
              className={`btn ${actionType === 'APPROVE' ? 'btn-success' : 'btn-outline'}`}
              style={{ padding: '0.75rem' }}
            >
              <CheckCircle2 size={18} /> Duyệt (Approve)
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
      </form>
    </Modal>
  );
};

export default TicketReviewModal;
