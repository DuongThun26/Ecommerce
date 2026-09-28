import React, { useState } from 'react';
import Modal from '../common/Modal';
import { ORDER_STATUS, ORDER_STATUS_LABELS } from '../../utils/constants';

export const OrderStatusModal = ({ isOpen, onClose, order, onUpdateStatus, loading = false }) => {
  const [selectedStatus, setSelectedStatus] = useState('');
  const [reason, setReason] = useState('');

  if (!order) return null;

  const validStatuses = [
    ORDER_STATUS.PROCESSING,
    ORDER_STATUS.SHIPPED,
    ORDER_STATUS.DELIVERED,
    ORDER_STATUS.COMPLETED,
    ORDER_STATUS.CANCELLED,
  ];

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!selectedStatus) return;
    onUpdateStatus(order.id, selectedStatus, reason);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Cập nhật trạng thái đơn hàng #${order.id}`}
      footer={
        <>
          <button type="button" onClick={onClose} className="btn btn-secondary">
            Hủy
          </button>
          <button
            type="submit"
            form="order-status-form"
            disabled={loading || !selectedStatus}
            className="btn btn-primary"
          >
            {loading ? 'Đang lưu...' : 'Cập nhật'}
          </button>
        </>
      }
    >
      <form id="order-status-form" onSubmit={handleSubmit}>
        <div style={{ marginBottom: '1rem', fontSize: '0.9rem' }}>
          <strong>Trạng thái hiện tại:</strong> {ORDER_STATUS_LABELS[order.status] || order.status}
        </div>

        <div className="form-group">
          <label className="form-label">Chọn trạng thái chuyển tiếp *</label>
          <select
            required
            value={selectedStatus}
            onChange={(e) => setSelectedStatus(e.target.value)}
            className="form-select"
          >
            <option value="">-- Chọn trạng thái mới --</option>
            {validStatuses.map((st) => (
              <option key={st} value={st}>
                {ORDER_STATUS_LABELS[st]} ({st})
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label className="form-label">Lý do / Ghi chú điều chỉnh</label>
          <textarea
            placeholder="Ví dụ: Đơn hàng đã bàn giao cho đơn vị vận chuyển ViettelPost"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="form-control"
            rows={3}
          />
        </div>
      </form>
    </Modal>
  );
};

export default OrderStatusModal;
