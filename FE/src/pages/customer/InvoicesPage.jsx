import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import invoiceService from '../../services/invoice.service';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import { INVOICE_STATUS_LABELS } from '../../utils/constants';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Pagination from '../../components/common/Pagination';
import { Receipt, Download, Eye, FileCheck } from 'lucide-react';
import { useToast } from '../../hooks/useToast';

export const InvoicesPage = () => {
  const { user } = useAuth();
  const { success, error } = useToast();
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [downloadingId, setDownloadingId] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const customerId = user?.customerId || user?.id || 1;

  const fetchInvoices = async () => {
    setLoading(true);
    try {
      const res = await invoiceService.getCustomerInvoices(customerId, {
        page,
        size: 10,
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
      console.error('Error fetching invoices:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvoices();
  }, [page]);

  const handleDownloadPdf = async (invoice) => {
    setDownloadingId(invoice.id);
    try {
      await invoiceService.downloadInvoicePdf(invoice.id, invoice.invoiceNumber);
      success(`Tải file PDF hóa đơn ${invoice.invoiceNumber} thành công!`);
    } catch (err) {
      error(err.message || 'Lỗi khi tải file PDF hóa đơn.');
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <div className="main-content animate-fade-in">
      <div className="page-header">
        <div>
          <h1 className="page-title">Hóa đơn điện tử VAT</h1>
          <p className="page-subtitle">Xem chi tiết và tải xuống hóa đơn PDF cho các đơn hàng đã thanh toán</p>
        </div>
      </div>

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
          <p style={{ fontSize: '1.1rem', fontWeight: 600 }}>Chưa có hóa đơn nào được phát hành.</p>
          <p style={{ fontSize: '0.875rem', marginTop: '0.25rem' }}>Hóa đơn sẽ tự động được tạo ngay khi đơn hàng thanh toán thành công.</p>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Số hóa đơn</th>
                <th>Đơn hàng</th>
                <th>Ngày phát hành</th>
                <th>Khách hàng</th>
                <th>Tổng tiền</th>
                <th>Trạng thái</th>
                <th style={{ textAlign: 'right' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {invoices.map((inv) => (
                <tr key={inv.id}>
                  <td>
                    <span style={{ fontWeight: 800, color: 'var(--color-primary)' }}>
                      {inv.invoiceNumber}
                    </span>
                  </td>
                  <td>
                    <Link to={`/orders/${inv.orderId}`} style={{ fontWeight: 600, color: 'var(--text-main)', textDecoration: 'underline' }}>
                      #{inv.orderId}
                    </Link>
                  </td>
                  <td>{formatDateTime(inv.issueDate || inv.createdAt)}</td>
                  <td>{inv.billingName}</td>
                  <td>
                    <strong style={{ color: 'var(--text-main)' }}>
                      {formatCurrency(inv.totalAmount)}
                    </strong>
                  </td>
                  <td>
                    <Badge variant={inv.status === 'ISSUED' ? 'success' : 'danger'}>
                      {INVOICE_STATUS_LABELS[inv.status] || inv.status}
                    </Badge>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: '0.5rem' }}>
                      <Link to={`/invoices/${inv.id}`} className="btn btn-sm btn-outline">
                        <Eye size={14} /> Xem
                      </Link>

                      <button
                        onClick={() => handleDownloadPdf(inv)}
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
    </div>
  );
};

export default InvoicesPage;
