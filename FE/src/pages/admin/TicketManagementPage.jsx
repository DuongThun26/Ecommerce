import React, { useState, useEffect } from 'react';
import Sidebar from '../../components/admin/Sidebar';
import TicketReviewModal from '../../components/admin/TicketReviewModal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Pagination from '../../components/common/Pagination';
import Badge from '../../components/common/Badge';
import ticketService from '../../services/ticket.service';
import categoryService from '../../services/category.service';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import { TICKET_STATUS, TICKET_STATUS_LABELS } from '../../utils/constants';
import { useToast } from '../../hooks/useToast';
import { useAuth } from '../../hooks/useAuth';
import { PackageCheck, Search, Filter, CheckCircle, RefreshCw, XCircle, Tag } from 'lucide-react';

export const TicketManagementPage = () => {
  const { success, error } = useToast();
  const { user, isAdmin, isProductOwner } = useAuth();
  const [tickets, setTickets] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('PENDING_APPROVAL');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const [selectedTicket, setSelectedTicket] = useState(null);
  const [isReviewOpen, setIsReviewOpen] = useState(false);
  const [reviewLoading, setReviewLoading] = useState(false);

  useEffect(() => {
    categoryService.getMyManagedCategories()
      .then((data) => {
        if (Array.isArray(data)) {
          setCategories(data);
        } else if (data?.content) {
          setCategories(data.content);
        }
      })
      .catch((err) => {
        console.error('Failed to load categories for ticket filter:', err);
        categoryService.getCategories({ size: 100 })
          .then(res => setCategories(res?.content || res || []))
          .catch(e => console.error(e));
      });
  }, []);

  const fetchTickets = async () => {
    setLoading(true);
    try {
      const res = await ticketService.getTickets({
        page,
        size: 10,
        status: statusFilter || undefined,
        categoryId: categoryFilter ? Number(categoryFilter) : undefined,
        keyword: keyword.trim() || undefined,
        sortBy: 'createdAt,desc',
      });
      let rawList = [];
      if (res && res.content) {
        rawList = res.content;
        setTotalPages(res.totalPages || 1);
      } else if (Array.isArray(res)) {
        rawList = res;
        setTotalPages(1);
      }

      // Safeguard: If PO and not Admin, ensure only tickets for this PO are shown
      if (!isAdmin() && isProductOwner() && user) {
        const u = (user.username || '').toLowerCase();
        const uid = user.id;
        rawList = rawList.filter(t => {
          if (t.managedByUserId && uid && t.managedByUserId === uid) return true;
          if (t.managedByUsername && t.managedByUsername.toLowerCase() === u) return true;
          if (t.orderItems && t.orderItems.some(it =>
            (it.managedByUserId && uid && it.managedByUserId === uid) ||
            (it.managedByUsername && it.managedByUsername.toLowerCase() === u)
          )) return true;
          return false;
        });
      }
      setTickets(rawList);
    } catch (err) {
      error(err.message || 'Không thể tải danh sách phiếu mua hàng.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTickets();
  }, [page, statusFilter, categoryFilter]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    fetchTickets();
  };

  const handleOpenReview = (ticket) => {
    setSelectedTicket(ticket);
    setIsReviewOpen(true);
  };

  const handleApprove = async (ticketId, notes) => {
    setReviewLoading(true);
    try {
      await ticketService.approveTicket(ticketId, notes);
      success(`Đã phê duyệt phiếu mua hàng #${selectedTicket.ticketCode}!`);
      setIsReviewOpen(false);
      fetchTickets();
    } catch (err) {
      error(err.message || 'Lỗi khi phê duyệt phiếu mua hàng.');
    } finally {
      setReviewLoading(false);
    }
  };

  const handleReject = async (ticketId, reason) => {
    setReviewLoading(true);
    try {
      await ticketService.rejectTicket(ticketId, reason);
      success(`Đã từ chối phiếu mua hàng #${selectedTicket.ticketCode}!`);
      setIsReviewOpen(false);
      fetchTickets();
    } catch (err) {
      error(err.message || 'Lỗi khi từ chối phiếu mua hàng.');
    } finally {
      setReviewLoading(false);
    }
  };

  const handleReturn = async (ticketId, reason) => {
    setReviewLoading(true);
    try {
      await ticketService.returnTicket(ticketId, reason);
      success(`Đã yêu cầu khách hàng sửa đổi phiếu mua hàng #${selectedTicket.ticketCode}!`);
      setIsReviewOpen(false);
      fetchTickets();
    } catch (err) {
      error(err.message || 'Lỗi khi trả lại phiếu mua hàng.');
    } finally {
      setReviewLoading(false);
    }
  };

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
    <div style={{ display: 'flex' }}>
      <Sidebar />

      <main style={{ flex: 1, padding: '2rem' }} className="animate-fade-in">
        <div className="page-header">
          <div>
            <h1 className="page-title">Duyệt Phiếu Mua Hàng (PO)</h1>
            <p className="page-subtitle">Xét duyệt các phiếu mua hàng gửi lên từ khách hàng</p>
          </div>
        </div>

        {/* Filter Bar */}
        <div style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: '1rem',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '1.5rem',
        }}>
          <form onSubmit={handleSearch} style={{ display: 'flex', gap: '0.5rem', flex: 1, maxWidth: '440px' }}>
            <div style={{ position: 'relative', flex: 1 }}>
              <Search size={18} style={{ position: 'absolute', left: '12px', top: '12px', color: 'var(--text-sub)' }} />
              <input
                type="text"
                placeholder="Tìm theo mã phiếu, tên KH, email..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                className="form-control"
                style={{ paddingLeft: '2.5rem' }}
              />
            </div>
            <button type="submit" className="btn btn-primary">Tìm kiếm</button>
          </form>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <Tag size={16} color="var(--text-muted)" />
              <select
                value={categoryFilter}
                onChange={(e) => { setCategoryFilter(e.target.value); setPage(0); }}
                className="form-select"
                style={{ width: 'auto' }}
              >
                <option value="">Tất cả loại hàng</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    [{c.code}] {c.name}
                  </option>
                ))}
              </select>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <Filter size={16} color="var(--text-muted)" />
              <select
                value={statusFilter}
                onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
                className="form-select"
                style={{ width: 'auto' }}
              >
                <option value="">Tất cả trạng thái</option>
                <option value="PENDING_APPROVAL">Chờ duyệt (PENDING_APPROVAL)</option>
                <option value="APPROVED">Đã duyệt (APPROVED)</option>
                <option value="RETURNED">Đã trả lại (RETURNED)</option>
                <option value="REJECTED">Đã từ chối (REJECTED)</option>
                <option value="CANCELLED">Đã hủy (CANCELLED)</option>
              </select>
            </div>
          </div>
        </div>

        {/* Table */}
        {loading ? (
          <LoadingSpinner text="Đang tải danh sách phiếu..." />
        ) : tickets.length === 0 ? (
          <div style={{
            textAlign: 'center',
            padding: '4rem 2rem',
            background: 'white',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border-color)',
            color: 'var(--text-muted)',
          }}>
            <PackageCheck size={48} style={{ margin: '0 auto 1rem auto', opacity: 0.6 }} />
            <p style={{ fontSize: '1.1rem', fontWeight: 600 }}>Không có phiếu mua hàng nào phù hợp bộ lọc.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Mã phiếu</th>
                  <th>Đơn con & Master Order</th>
                  <th>PO Phụ trách</th>
                  <th>Khách hàng</th>
                  <th>Tổng đơn con</th>
                  <th>Ngày nộp</th>
                  <th>Trạng thái</th>
                  <th style={{ textAlign: 'right' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {tickets.map((t) => (
                  <tr key={t.id}>
                    <td>
                      <strong style={{ color: 'var(--color-primary)' }}>{t.ticketCode}</strong>
                    </td>
                    <td>
                      {t.subOrderCode ? (
                        <div>
                          <span style={{ fontWeight: 700, color: '#0284c7' }}>{t.subOrderCode}</span>
                          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Master: #{t.orderId}</div>
                        </div>
                      ) : (
                        <span>#{t.orderId}</span>
                      )}
                    </td>
                    <td>
                      <span style={{ fontWeight: 600 }}>{t.managedByUsername || 'Chung / Admin'}</span>
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{t.customerName}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{t.customerEmail}</div>
                    </td>
                    <td><strong style={{ color: 'var(--text-main)' }}>{formatCurrency(t.totalAmount)}</strong></td>
                    <td>{formatDateTime(t.createdAt)}</td>
                    <td>
                      <Badge variant={getStatusBadgeVariant(t.status)}>
                        {TICKET_STATUS_LABELS[t.status] || t.status}
                      </Badge>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        onClick={() => handleOpenReview(t)}
                        className={`btn btn-sm ${t.status === 'PENDING_APPROVAL' ? 'btn-primary' : 'btn-outline'}`}
                      >
                        {t.status === 'PENDING_APPROVAL' ? 'Xét duyệt phiếu' : 'Xem chi tiết'}
                      </button>
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

        {/* Review Modal */}
        <TicketReviewModal
          isOpen={isReviewOpen}
          onClose={() => setIsReviewOpen(false)}
          ticket={selectedTicket}
          onApprove={handleApprove}
          onReject={handleReject}
          onReturn={handleReturn}
          loading={reviewLoading}
        />
      </main>
    </div>
  );
};

export default TicketManagementPage;
