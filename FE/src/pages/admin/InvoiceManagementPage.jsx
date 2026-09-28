import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Sidebar from '../../components/admin/Sidebar';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Pagination from '../../components/common/Pagination';
import Badge from '../../components/common/Badge';
import invoiceService from '../../services/invoice.service';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import { INVOICE_STATUS_LABELS } from '../../utils/constants';
import { useToast } from '../../hooks/useToast';
import { Receipt, Search, Download, Eye, PlusCircle, Filter } from 'lucide-react';
import Modal from '../../components/common/Modal';

export const InvoiceManagementPage = () => {
  const { success, error } = useToast();
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [downloadingId, setDownloadingId] = useState(null);

  // Manual generation modal state
  const [isGenerateOpen, setIsGenerateOpen] = useState(false);
  const [manualOrderId, setManualOrderId] = useState('');
  const [generateLoading, setGenerateLoading] = useState(false);

  const fetchInvoices = async () => {
    setLoading(true);
    try {
      const res = await invoiceService.getInvoices({
        page,
        size: 10,
        keyword: keyword.trim() || undefined,
        status: statusFilter || undefined,
        sortBy: 'createdAt,desc',
      });
      if (res && res.content) {
        setInvoices(res.content);
        setTotalPages(res.totalPages || 1);
      } else if (Array.isArray(res)) {
        setInvoices(res);
        setTotalPages(1);
      }
    } catch (err) {
      error(err.message || 'Không thể tải danh sách hóa đơn.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvoices();
  }, [page, statusFilter]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    fetchInvoices();
  };

  const handleDownload = async (inv) => {
    setDownloadingId(inv.id);
    try {
      await invoiceService.downloadInvoicePdf(inv.id, inv.invoiceNumber);
      success(`Tải file PDF hóa đơn ${inv.invoiceNumber} thành công!`);
    } catch (err) {
      error(err.message || 'Lỗi khi tải file PDF.');
    } finally {
      setDownloadingId(null);
    }
  };

  const handleManualGenerate = async (e) => {
    e.preventDefault();
    if (!manualOrderId) return;
    setGenerateLoading(true);
    try {
      const inv = await invoiceService.generateInvoiceForOrder(manualOrderId);
      success(`Phát hành hóa đơn ${inv?.invoiceNumber || ''} cho đơn hàng #${manualOrderId} thành công!`);
      setIsGenerateOpen(false);
      setManualOrderId('');
      fetchInvoices();
    } catch (err) {
      error(err.message || 'Lỗi khi phát hành hóa đơn.');
    } finally {
      setGenerateLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex' }}>
      <Sidebar />

      <main style={{ flex: 1, padding: '2rem' }} className="animate-fade-in">
        <div className="page-header">
          <div>
            <h1 className="page-title">Quản lý Hóa đơn VAT</h1>
            <p className="page-subtitle">Hồ sơ hóa đơn tài chính snapshot bất biến và quản lý xuất file PDF</p>
          </div>

          <button onClick={() => setIsGenerateOpen(true)} className="btn btn-primary">
            <PlusCircle size={18} /> Phát hành hóa đơn thủ công
          </button>
        </div>

        {/* Search & Filter Bar */}
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
                placeholder="Tìm theo số hóa đơn, tên KH, email..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                className="form-control"
                style={{ paddingLeft: '2.5rem' }}
              />
            </div>
            <button type="submit" className="btn btn-primary">Tìm</button>
          </form>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Filter size={18} color="var(--text-muted)" />
            <select
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
              className="form-select"
              style={{ width: 'auto' }}
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ISSUED">Đã phát hành (ISSUED)</option>
              <option value="CANCELLED">Đã hủy (CANCELLED)</option>
            </select>
          </div>
        </div>

        {/* Table */}
        {loading ? (
          <LoadingSpinner text="Đang tải danh sách hóa đơn..." />
        ) : invoices.length === 0 ? (
          <div style={{
            textAlign: 'center',
            padding: '4rem 2rem',
            background: 'white',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border-color)',
            color: 'var(--text-muted)',
          }}>
            <Receipt size={48} style={{ margin: '0 auto 1rem auto', opacity: 0.6 }} />
            <p style={{ fontSize: '1.1rem', fontWeight: 600 }}>Không tìm thấy hóa đơn nào.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Số hóa đơn</th>
                  <th>Đơn hàng</th>
                  <th>Khách hàng</th>
                  <th>Ngày phát hành</th>
                  <th>Tổng tiền</th>
                  <th>Trạng thái</th>
                  <th style={{ textAlign: 'right' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {invoices.map((inv) => (
                  <tr key={inv.id}>
                    <td><strong style={{ color: 'var(--color-primary)' }}>{inv.invoiceNumber}</strong></td>
                    <td>
                      <Link to={`/orders/${inv.orderId}`} style={{ fontWeight: 600, textDecoration: 'underline' }}>
                        #{inv.orderId}
                      </Link>
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{inv.billingName}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{inv.billingEmail}</div>
                    </td>
                    <td>{formatDateTime(inv.issueDate || inv.createdAt)}</td>
                    <td><strong style={{ color: 'var(--text-main)' }}>{formatCurrency(inv.totalAmount)}</strong></td>
                    <td>
                      <Badge variant={inv.status === 'ISSUED' ? 'success' : 'danger'}>
                        {INVOICE_STATUS_LABELS[inv.status] || inv.status}
                      </Badge>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '0.5rem' }}>
                        <Link to={`/invoices/${inv.id}`} className="btn btn-sm btn-outline">
                          <Eye size={14} /> Chi tiết
                        </Link>
                        <button
                          onClick={() => handleDownload(inv)}
                          disabled={downloadingId === inv.id}
                          className="btn btn-sm btn-primary"
                        >
                          <Download size={14} /> {downloadingId === inv.id ? 'Đang tải...' : 'Tải PDF'}
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

        {/* Manual Generate Modal */}
        <Modal
          isOpen={isGenerateOpen}
          onClose={() => setIsGenerateOpen(false)}
          title="Phát hành hóa đơn bổ sung cho đơn hàng"
          footer={
            <>
              <button type="button" onClick={() => setIsGenerateOpen(false)} className="btn btn-secondary">
                Hủy
              </button>
              <button
                type="submit"
                form="manual-invoice-form"
                disabled={generateLoading || !manualOrderId}
                className="btn btn-primary"
              >
                {generateLoading ? 'Đang phát hành...' : 'Phát hành ngay'}
              </button>
            </>
          }
        >
          <form id="manual-invoice-form" onSubmit={handleManualGenerate}>
            <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
              Nhập mã đơn hàng (Order ID) đã hoàn tất thanh toán (PAID) để hệ thống tự động sinh số hóa đơn và snapshot thông tin.
            </p>
            <div className="form-group">
              <label className="form-label">Mã đơn hàng (Order ID) *</label>
              <input
                type="number"
                required
                placeholder="Ví dụ: 10"
                value={manualOrderId}
                onChange={(e) => setManualOrderId(e.target.value)}
                className="form-control"
              />
            </div>
          </form>
        </Modal>
      </main>
    </div>
  );
};

export default InvoiceManagementPage;
