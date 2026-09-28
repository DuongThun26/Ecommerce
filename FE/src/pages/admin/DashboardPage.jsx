import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Sidebar from '../../components/admin/Sidebar';
import StatCard from '../../components/admin/StatCard';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import productService from '../../services/product.service';
import ticketService from '../../services/ticket.service';
import orderService from '../../services/order.service';
import invoiceService from '../../services/invoice.service';
import { formatCurrency } from '../../utils/formatters';
import {
  Package,
  PackageCheck,
  ShoppingBag,
  Receipt,
  DollarSign,
  TrendingUp,
  ArrowRight
} from 'lucide-react';

export const DashboardPage = () => {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    totalProducts: 0,
    pendingTickets: 0,
    totalOrders: 0,
    totalInvoices: 0,
    totalRevenue: 0,
  });

  useEffect(() => {
    const fetchStats = async () => {
      setLoading(true);
      try {
        const [productsRes, ticketsRes, ordersRes, invoicesRes] = await Promise.allSettled([
          productService.getProducts({ page: 0, size: 1 }),
          ticketService.getTickets({ status: 'PENDING_APPROVAL', page: 0, size: 1 }),
          orderService.getOrders({ page: 0, size: 1 }),
          invoiceService.getInvoices({ page: 0, size: 10 }),
        ]);

        const totalProducts = productsRes.status === 'fulfilled' ? (productsRes.value?.totalElements || 0) : 0;
        const pendingTickets = ticketsRes.status === 'fulfilled' ? (ticketsRes.value?.totalElements || 0) : 0;
        const totalOrders = ordersRes.status === 'fulfilled' ? (ordersRes.value?.totalElements || 0) : 0;
        const totalInvoices = invoicesRes.status === 'fulfilled' ? (invoicesRes.value?.totalElements || 0) : 0;
        
        let totalRevenue = 0;
        if (invoicesRes.status === 'fulfilled' && invoicesRes.value?.content) {
          totalRevenue = invoicesRes.value.content.reduce((sum, inv) => sum + (Number(inv.totalAmount) || 0), 0);
        }

        setStats({
          totalProducts,
          pendingTickets,
          totalOrders,
          totalInvoices,
          totalRevenue,
        });
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  return (
    <div style={{ display: 'flex' }}>
      <Sidebar />

      <main style={{ flex: 1, padding: '2rem' }} className="animate-fade-in">
        <div className="page-header">
          <div>
            <h1 className="page-title">Bảng điều khiển Quản trị</h1>
            <p className="page-subtitle">Tổng quan số liệu sản phẩm, duyệt phiếu mua hàng PO và phát hành hóa đơn</p>
          </div>
        </div>

        {loading ? (
          <LoadingSpinner text="Đang tải dữ liệu tổng quan..." />
        ) : (
          <>
            {/* Stat Cards Grid */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
              gap: '1.5rem',
              marginBottom: '2rem',
            }}>
              <StatCard
                title="Phiếu chờ PO duyệt"
                value={stats.pendingTickets}
                subtitle="Cần xét duyệt ngay"
                icon={PackageCheck}
                color="#f59e0b"
              />

              <StatCard
                title="Tổng đơn hàng"
                value={stats.totalOrders}
                subtitle="Tất cả trạng thái"
                icon={ShoppingBag}
                color="#4f46e5"
              />

              <StatCard
                title="Hóa đơn đã xuất"
                value={stats.totalInvoices}
                subtitle="Hóa đơn VAT hợp lệ"
                icon={Receipt}
                color="#10b981"
              />

              <StatCard
                title="Tổng sản phẩm"
                value={stats.totalProducts}
                subtitle="Đang lưu trong kho"
                icon={Package}
                color="#0ea5e9"
              />
            </div>

            {/* Quick Actions Grid */}
            <div className="grid-2">
              <div className="card">
                <div className="card-header">
                  <h3 className="card-title">Duyệt Phiếu Mua Hàng (PO)</h3>
                </div>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '1.25rem' }}>
                  Xét duyệt các phiếu mua hàng do khách hàng nộp lên để quyết định phê duyệt (chuyển trạng thái sang thanh toán), yêu cầu sửa đổi hoặc từ chối.
                </p>
                <Link to="/admin/tickets" className="btn btn-primary">
                  Xem danh sách phiếu PO <ArrowRight size={16} />
                </Link>
              </div>

              <div className="card">
                <div className="card-header">
                  <h3 className="card-title">Quản lý Hóa đơn VAT</h3>
                </div>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '1.25rem' }}>
                  Tra cứu lịch sử hóa đơn tài chính, tải file PDF snapshot hoặc phát hành hóa đơn bổ sung cho các đơn hàng hợp lệ.
                </p>
                <Link to="/admin/invoices" className="btn btn-secondary">
                  Quản lý hóa đơn <ArrowRight size={16} />
                </Link>
              </div>
            </div>
          </>
        )}
      </main>
    </div>
  );
};

export default DashboardPage;
