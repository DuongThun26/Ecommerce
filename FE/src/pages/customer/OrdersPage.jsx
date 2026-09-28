import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import orderService from '../../services/order.service';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import { ORDER_STATUS_LABELS, ORDER_STATUS_BADGE } from '../../utils/constants';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Pagination from '../../components/common/Pagination';
import { ShoppingBag, Eye, Calendar, DollarSign } from 'lucide-react';

export const OrdersPage = () => {
  const { user } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const customerId = user?.customerId || user?.id || 1;

  const fetchOrders = async () => {
    setLoading(true);
    try {
      let res;
      if (user?.role === 'ADMIN' || user?.role === 'PRODUCT_OWNER') {
        res = await orderService.getOrders({
          page,
          size: 10,
          status: statusFilter || undefined,
          sortBy: 'createdAt,desc',
        });
      } else {
        try {
          res = await orderService.getMyOrders({
            page,
            size: 10,
            status: statusFilter || undefined,
            sortBy: 'createdAt,desc',
          });
        } catch {
          try {
            res = await orderService.getCustomerOrders(customerId, {
              page,
              size: 10,
              status: statusFilter || undefined,
              sortBy: 'createdAt,desc',
            });
          } catch {
            res = await orderService.getOrders({
              page,
              size: 10,
              status: statusFilter || undefined,
              sortBy: 'createdAt,desc',
            });
          }
        }
      }

      if (res && res.content) {
        setOrders(res.content);
        setTotalPages(res.totalPages || 1);
      } else if (Array.isArray(res)) {
        setOrders(res);
        setTotalPages(1);
      } else if (res && res.data && res.data.content) {
        setOrders(res.data.content);
        setTotalPages(res.data.totalPages || 1);
      } else {
        setOrders([]);
      }
    } catch (err) {
      console.error('Error fetching orders:', err);
      setOrders([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, [page, statusFilter]);

  return (
    <div className="main-content animate-fade-in">
      <div className="page-header">
        <div>
          <h1 className="page-title">Đơn hàng của tôi</h1>
          <p className="page-subtitle">Theo dõi trạng thái duyệt PO, thanh toán và lịch sử giao hàng</p>
        </div>

        <div>
          <select
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
            className="form-select"
            style={{ minWidth: '180px' }}
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
          <p style={{ fontSize: '1.1rem', fontWeight: 600 }}>Chưa có đơn hàng nào.</p>
          <Link to="/" className="btn btn-primary" style={{ marginTop: '1rem' }}>
            Mua sắm ngay
          </Link>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Mã đơn</th>
                <th>Ngày tạo</th>
                <th>Số lượng SP</th>
                <th>Tổng tiền</th>
                <th>Trạng thái</th>
                <th style={{ textAlign: 'right' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.id}>
                  <td><strong>#{order.id}</strong></td>
                  <td>{formatDateTime(order.createdAt)}</td>
                  <td>{order.orderItems?.length || 0} sản phẩm</td>
                  <td>
                    <strong style={{ color: 'var(--color-primary)' }}>
                      {formatCurrency(order.totalAmount)}
                    </strong>
                  </td>
                  <td>
                    <Badge variant={ORDER_STATUS_BADGE[order.status] || 'neutral'}>
                      {ORDER_STATUS_LABELS[order.status] || order.status}
                    </Badge>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <Link to={`/orders/${order.id}`} className="btn btn-sm btn-outline">
                      <Eye size={14} /> Chi tiết
                    </Link>
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
    </div>
  );
};

export default OrdersPage;
