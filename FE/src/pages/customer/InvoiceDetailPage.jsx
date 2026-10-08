import React, { useState, useEffect } from 'react';
import { useParams, useLocation, Link } from 'react-router-dom';
import invoiceService from '../../services/invoice.service';
import { useToast } from '../../hooks/useToast';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import { INVOICE_STATUS_LABELS } from '../../utils/constants';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { ArrowLeft, Download, Receipt, FileText, CheckCircle2 } from 'lucide-react';

export const InvoiceDetailPage = () => {
  const { id } = useParams();
  const location = useLocation();
  const { success, error } = useToast();
  const [invoice, setInvoice] = useState(null);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);

  const fetchInvoice = async () => {
    setLoading(true);
    try {
      const data = await invoiceService.getInvoiceById(id);
      setInvoice(data);
    } catch (err) {
      error(err.message || 'Không thể tải chi tiết hóa đơn.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvoice();
    const queryParams = new URLSearchParams(location.search);
    if (queryParams.get('payment') === 'success') {
      success('Thanh toán đơn hàng thành công! Hóa đơn điện tử VAT của bạn đã sẵn sàng.');
    }
  }, [id, location.search]);

  const handleDownload = async () => {
    setDownloading(true);
    try {
      await invoiceService.downloadInvoicePdf(invoice.id, invoice.invoiceNumber);
      success(`Đã tải file PDF hóa đơn ${invoice.invoiceNumber}`);
    } catch (err) {
      error(err.message || 'Lỗi khi tải file PDF.');
    } finally {
      setDownloading(false);
    }
  };

  if (loading) {
    return <LoadingSpinner fullPage text="Đang tải hóa đơn..." />;
  }

  if (!invoice) {
    return (
      <div className="main-content" style={{ textAlign: 'center', padding: '4rem 1rem' }}>
        <h2>Không tìm thấy hóa đơn #{id}</h2>
        <Link to="/invoices" className="btn btn-primary" style={{ marginTop: '1rem' }}>
          Quay lại danh sách
        </Link>
      </div>
    );
  }

  return (
    <div className="main-content animate-fade-in" style={{ maxWidth: '900px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <Link to="/invoices" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-muted)', fontSize: '0.9rem', fontWeight: 600 }}>
          <ArrowLeft size={16} /> Danh sách hóa đơn
        </Link>

        <button
          onClick={handleDownload}
          disabled={downloading}
          className="btn btn-primary btn-lg"
        >
          <Download size={18} /> {downloading ? 'Đang tạo & tải file...' : 'Tải Hóa Đơn PDF'}
        </button>
      </div>

      {/* Invoice Document Paper Style Card */}
      <div className="card" style={{ padding: '2.5rem 2rem', background: 'white', border: '1px solid var(--border-color)', boxShadow: 'var(--shadow-lg)' }}>
        {/* Header Section */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: '2px solid var(--border-color)', paddingBottom: '1.5rem', marginBottom: '2rem', flexWrap: 'wrap', gap: '1.5rem' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <Receipt size={24} color="var(--color-primary)" />
              <span style={{ fontSize: '1.3rem', fontWeight: 800, color: 'var(--color-primary)' }}>
                E-COMMERCE ENTERPRISE
              </span>
            </div>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Tân Mỹ, Nam Từ Liêm, TP Hà Nội</p>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Hotline: +84 976 570 703 | Email: thuandv@ecommerce.local</p>
          </div>

          <div style={{ textAlign: 'right' }}>
            <h2 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-main)', letterSpacing: '-0.02em' }}>
              HÓA ĐƠN BÁN HÀNG
            </h2>
            <div style={{ fontSize: '1.1rem', fontWeight: 800, color: 'var(--color-primary)', marginTop: '0.25rem' }}>
              #{invoice.invoiceNumber}
            </div>
            <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
              Ngày xuất: {formatDateTime(invoice.issueDate)}
            </div>
            <div style={{ marginTop: '0.5rem' }}>
              <Badge variant={invoice.status === 'ISSUED' ? 'success' : 'danger'}>
                {INVOICE_STATUS_LABELS[invoice.status] || invoice.status}
              </Badge>
            </div>
          </div>
        </div>

        {/* Bill To & Order Info Grid */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem', marginBottom: '2rem', background: '#f8fafc', padding: '1.25rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
          <div>
            <h4 style={{ fontSize: '0.8rem', fontWeight: 800, textTransform: 'uppercase', color: 'var(--color-primary)', marginBottom: '0.5rem', letterSpacing: '0.04em' }}>
              Khách hàng (Bill To):
            </h4>
            <div style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--text-main)' }}>{invoice.billingName}</div>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>Email: {invoice.billingEmail}</div>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>SĐT: {invoice.billingPhone}</div>
            {invoice.billingAddress && (
              <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>Địa chỉ: {invoice.billingAddress}</div>
            )}
          </div>

          <div>
            <h4 style={{ fontSize: '0.8rem', fontWeight: 800, textTransform: 'uppercase', color: 'var(--color-primary)', marginBottom: '0.5rem', letterSpacing: '0.04em' }}>
              Thông tin đơn hàng:
            </h4>
            <div style={{ fontSize: '0.875rem' }}>
              Mã đơn hàng: <Link to={`/orders/${invoice.orderId}`} style={{ fontWeight: 700, color: 'var(--color-primary)' }}>#{invoice.orderId}</Link>
            </div>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
              Phương thức: {invoice.paymentMethod || 'VNPAY'}
            </div>
            {invoice.paymentTransactionRef && (
              <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                Mã GD VNPay: {invoice.paymentTransactionRef}
              </div>
            )}
          </div>
        </div>

        {/* Item Table Snapshot */}
        <div className="table-responsive" style={{ marginBottom: '2rem' }}>
          <table className="custom-table">
            <thead>
              <tr>
                <th style={{ width: '50px' }}>#</th>
                <th>Tên sản phẩm (Snapshot)</th>
                <th style={{ textAlign: 'center', width: '80px' }}>SL</th>
                <th style={{ textAlign: 'right' }}>Đơn giá</th>
                <th style={{ textAlign: 'right' }}>Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              {invoice.items?.map((item, index) => (
                <tr key={item.id || index}>
                  <td>{index + 1}</td>
                  <td><strong>{item.productName}</strong></td>
                  <td style={{ textAlign: 'center' }}>{item.quantity}</td>
                  <td style={{ textAlign: 'right' }}>{formatCurrency(item.unitPrice)}</td>
                  <td style={{ textAlign: 'right', fontWeight: 700 }}>
                    {formatCurrency(item.subtotal)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Totals Summary */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '2rem' }}>
          <div style={{ width: '100%', maxWidth: '340px', display: 'flex', flexDirection: 'column', gap: '0.5rem', fontSize: '0.95rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
              <span>Tạm tính (Subtotal):</span>
              <span>{formatCurrency(invoice.subtotal)}</span>
            </div>
            {invoice.discountAmount > 0 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--color-danger)' }}>
                <span>Giảm giá:</span>
                <span>-{formatCurrency(invoice.discountAmount)}</span>
              </div>
            )}
            {invoice.taxAmount > 0 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
                <span>Thuế VAT:</span>
                <span>{formatCurrency(invoice.taxAmount)}</span>
              </div>
            )}
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingTop: '0.75rem', borderTop: '2px solid var(--border-color)', fontWeight: 800, fontSize: '1.25rem', color: 'var(--color-primary)' }}>
              <span>Tổng thanh toán:</span>
              <span>{formatCurrency(invoice.totalAmount)}</span>
            </div>
          </div>
        </div>

        {/* Footnote */}
        <div style={{ textAlign: 'center', borderTop: '1px dashed var(--border-color)', paddingTop: '1.5rem', fontSize: '0.8rem', color: 'var(--text-sub)' }}>
          Hóa đơn điện tử được tạo tự động bởi hệ thống E-Commerce Enterprise. Mọi thắc mắc xin vui lòng liên hệ bộ phận hỗ trợ khách hàng.
        </div>
      </div>
    </div>
  );
};

export default InvoiceDetailPage;
