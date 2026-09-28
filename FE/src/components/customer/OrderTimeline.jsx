import React from 'react';
import { formatDateTime } from '../../utils/formatters';
import { ORDER_STATUS_LABELS } from '../../utils/constants';
import { CheckCircle2, Clock, XCircle, RefreshCw, UserCheck } from 'lucide-react';

export const OrderTimeline = ({ history = [] }) => {
  if (!history || history.length === 0) {
    return <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Chưa có lịch sử trạng thái.</p>;
  }

  const getStatusIcon = (status) => {
    switch (status) {
      case 'PAID':
      case 'COMPLETED':
      case 'DELIVERED':
        return <CheckCircle2 size={16} color="var(--color-success)" />;
      case 'CANCELLED':
      case 'REJECTED':
        return <XCircle size={16} color="var(--color-danger)" />;
      case 'RETURNED':
        return <RefreshCw size={16} color="var(--color-warning)" />;
      case 'PENDING_APPROVAL':
        return <UserCheck size={16} color="var(--color-primary)" />;
      default:
        return <Clock size={16} color="var(--color-info)" />;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', position: 'relative', paddingLeft: '1.5rem' }}>
      {/* Line connecting nodes */}
      <div style={{
        position: 'absolute',
        top: '10px',
        bottom: '10px',
        left: '7px',
        width: '2px',
        background: 'var(--border-color)',
      }} />

      {history.map((item, idx) => (
        <div key={item.id || idx} style={{ position: 'relative', display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
          {/* Node dot */}
          <div style={{
            position: 'absolute',
            left: '-1.5rem',
            top: '2px',
            background: 'white',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1,
          }}>
            {getStatusIcon(item.toStatus)}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
            <span style={{ fontWeight: 700, fontSize: '0.9rem', color: 'var(--text-main)' }}>
              {ORDER_STATUS_LABELS[item.toStatus] || item.toStatus}
            </span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              ({formatDateTime(item.createdAt)})
            </span>
          </div>

          <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
            Người thực hiện: <strong style={{ color: 'var(--text-main)' }}>{item.actorType || 'SYSTEM'}</strong>
            {item.actorIdentifier && ` (${item.actorIdentifier})`}
          </div>

          {item.reason && (
            <div style={{
              fontSize: '0.825rem',
              color: '#334155',
              background: '#f8fafc',
              padding: '0.4rem 0.6rem',
              borderRadius: 'var(--radius-sm)',
              borderLeft: '3px solid var(--border-focus)',
              marginTop: '0.2rem',
            }}>
              Ghi chú: {item.reason}
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

export default OrderTimeline;
