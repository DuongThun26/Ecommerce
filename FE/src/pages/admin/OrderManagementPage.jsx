import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Sidebar from '../../components/admin/Sidebar';
import OrderStatusModal from '../../components/admin/OrderStatusModal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Pagination from '../../components/common/Pagination';
import Badge from '../../components/common/Badge';
import orderService from '../../services/order.service';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import { ORDER_STATUS_LABELS, ORDER_STATUS_BADGE } from '../../utils/constants';
import { useToast } from '../../hooks/useToast';
import { ShoppingBag, Filter, Eye, RefreshCw } from 'lucide-react';

export const OrderManagementPage = () => {
  const { success, error } = useToast();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const [selectedOrder, setSelectedOrder] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const res = await orderService.getOrders({
        page,
        size: 10,
        status: statusFilter || undefined,
        sortBy: 'createdAt,desc',
      });
      if (res && res.content) {
        setOrders(res.content);
        setTotalPages(res.totalPages || 1);
      } else if (Array.isArray(res)) {
        setOrders(res);
        setTotalPages(1);
      }
    } catch (err) {
      error(err.message || 'Không thể tải danh sách đơn hàng.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, [page, statusFilter]);

  const handleOpenStatusModal = (order) => {
    setSelectedOrder(order);
    setIsModalOpen(true);
  };

  const handleUpdateStatus = async (orderId, status, reason) => {
    setActionLoading(true);
    try {
      await orderService.updateOrderStatus(orderId, status, reason);
      success(`Đã cập nhật trạng thái đơn hàng #${orderId} sang ${ORDER_STATUS_LABELS[status] || status}!`);
      setIsModalOpen(false);
      fetchOrders();
    } catch (err) {
      error(err.message || 'Lỗi khi cập nhật trạng thái đơn hàng.');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex' }}>
      <Sidebar />

      <main style={{ flex: 1, padding: '2rem' }} className="animate-fade-in">
        <div className="page-header">
          <div>
            <h1 className="page-title">Quản lý Đơn hàng</h1>
            <p className="page-subtitle">Theo dõi toàn bộ đơn hàng và cập nhật tiến trình xử lý, giao hàng</p>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Filter size={18} color="var(--text-muted)" />
            <select
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
              className="form-select"
              style={{ width: 'auto' }}
            >
              <option value="">Tất cả trạng thái</option>
              {Object.entries(ORDER_STATUS_LABELS).map(([key, label]) => (
                <option key={key} value={key}>{label}</option>
              ))}
            </select>
          </div>
        </div>

        {loading ? (
          <LoadingSpinner text="Đang tải danh sách đơn hàng..." />
        ) : orders.length === 0 ? (
          <div style={{
            textAlign: 'center',
            padding: '4rem 2rem',
            background: 'white',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border-color)',
            color: 'var(--text-muted)',
          }}>
            <ShoppingBag size={48} style={{ margin: '0 auto 1rem auto', opacity: 0.6 }} />
            <p style={{ fontSize: '1.1rem', fontWeight: 600 }}>Không tìm thấy đơn hàng nào.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Mã đơn</th>
                  <th>Khách hàng</th>
                  <th>Ngày đặt</th>
                  <th>Tổng tiền</th>
                  <th>Trạng thái</th>
                  <th style={{ textAlign: 'right' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id}>
                    <td><strong>#{order.id}</strong></td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{order.customer?.name || `Customer #${order.customerId || ''}`}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{order.customer?.email}</div>
                    </td>
                    <td>{formatDateTime(order.createdAt)}</td>
                    <td><strong style={{ color: 'var(--color-primary)' }}>{formatCurrency(order.totalAmount)}</strong></td>
                    <td>
                      <Badge variant={ORDER_STATUS_BADGE[order.status] || 'neutral'}>
                        {ORDER_STATUS_LABELS[order.status] || order.status}
                      </Badge>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '0.5rem' }}>
                        <Link to={`/orders/${order.id}`} className="btn btn-sm btn-outline">
                          <Eye size={14} /> Chi tiết
                        </Link>
                        <button
                          onClick={() => handleOpenStatusModal(order)}
                          className="btn btn-sm btn-primary"
                        >
                          <RefreshCw size={14} /> Đổi trạng thái
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <Pagination
              currentPage={page}
              totalPages={totalPages}
              onPageChange={(newPage) => setPage(newPage)}
            />
          </div>
        )}

        <OrderStatusModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          order={selectedOrder}
          onUpdateStatus={handleUpdateStatus}
          loading={actionLoading}
        />
      </main>
    </div>
  );
};

export default OrderManagementPage;
