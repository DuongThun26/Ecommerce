import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import paymentService from '../../services/payment.service';
import invoiceService from '../../services/invoice.service';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { CheckCircle2, XCircle, ArrowRight, Receipt, ShoppingBag, ArrowLeft } from 'lucide-react';

export const PaymentResultPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [result, setResult] = useState(null);
  const [invoice, setInvoice] = useState(null);
  const [countdown, setCountdown] = useState(3);

  useEffect(() => {
    let isMounted = true;

    const verifyPayment = async () => {
      setLoading(true);
      try {
        const res = await paymentService.handleVnPayReturn(location.search);
        if (!isMounted) return;
        setResult(res);

        const isSuccess = res?.status === 'SUCCESS' || res?.RspCode === '00';
        if (isSuccess) {
          // Lấy thông tin hóa đơn nếu có orderId hoặc invoiceId
          try {
            if (res?.invoiceId) {
              const inv = await invoiceService.getInvoiceById(res.invoiceId);
              if (isMounted) setInvoice(inv);
            } else if (res?.orderId) {
              const inv = await invoiceService.getInvoiceByOrderId(res.orderId);
              if (isMounted) setInvoice(inv);
            }
          } catch (invErr) {
            console.warn('Could not immediately fetch invoice for order:', invErr);
          }
        }
      } catch (err) {
        if (!isMounted) return;
        setResult({
          status: 'FAILED',
          message: err.message || 'Không thể xác thực giao dịch thanh toán.',
        });
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    verifyPayment();

    return () => {
      isMounted = false;
    };
  }, [location.search]);

  // Tự động chuyển hướng về trang hóa đơn khi thanh toán thành công
  useEffect(() => {
    const isSuccess = result?.status === 'SUCCESS' || result?.RspCode === '00';
    if (!loading && isSuccess) {
      const timer = setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) {
            clearInterval(timer);
            const targetPath = invoice?.id ? `/invoices/${invoice.id}` : '/invoices';
            navigate(targetPath);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);

      return () => clearInterval(timer);
    }
  }, [loading, result, invoice, navigate]);

  if (loading) {
    return <LoadingSpinner fullPage text="Đang xác thực kết quả thanh toán VNPay và phát hành hóa đơn..." />;
  }

  const isSuccess = result?.status === 'SUCCESS' || result?.RspCode === '00';
  const targetInvoicePath = invoice?.id ? `/invoices/${invoice.id}` : '/invoices';

  return (
    <div className="main-content" style={{ minHeight: '75vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '2rem 1rem' }}>
      <div className="card animate-fade-in" style={{ width: '100%', maxWidth: '540px', textAlign: 'center', padding: '2.5rem 2rem', boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)' }}>
        <div style={{
          width: '76px',
          height: '76px',
          borderRadius: '50%',
          background: isSuccess ? 'var(--color-success-light)' : 'var(--color-danger-light)',
          color: isSuccess ? 'var(--color-success)' : 'var(--color-danger)',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: '1.5rem',
        }}>
          {isSuccess ? <CheckCircle2 size={46} /> : <XCircle size={46} />}
        </div>

        <h2 style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '0.5rem' }}>
          {isSuccess ? 'Thanh toán thành công!' : 'Thanh toán không thành công'}
        </h2>

        <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem', marginBottom: '1.5rem', lineHeight: 1.5 }}>
          {result?.message || (isSuccess ? 'Đơn hàng của bạn đã được thanh toán và hóa đơn điện tử VAT đã được phát hành.' : 'Giao dịch bị gián đoạn hoặc bị hủy bởi người dùng.')}
        </p>

        {isSuccess && (
          <div style={{
            background: 'var(--bg-secondary)',
            padding: '1rem',
            borderRadius: 'var(--radius-md)',
            border: '1px solid var(--border-color)',
            marginBottom: '1.5rem',
            textAlign: 'left',
            fontSize: '0.9rem'
          }}>
            {result?.orderId && (
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.35rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Mã đơn hàng:</span>
                <strong style={{ color: 'var(--text-main)' }}>#{result.orderId}</strong>
              </div>
            )}
            {invoice?.invoiceNumber && (
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.35rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Số hóa đơn VAT:</span>
                <strong style={{ color: 'var(--color-primary)' }}>{invoice.invoiceNumber}</strong>
              </div>
            )}
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '0.5rem', paddingTop: '0.5rem', borderTop: '1px dashed var(--border-color)', color: 'var(--color-primary)', fontWeight: 600, fontSize: '0.85rem' }}>
              <span>Tự động chuyển về hóa đơn sau:</span>
              <span><strong>{countdown}s</strong></span>
            </div>
          </div>
        )}

        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {isSuccess ? (
            <>
              <button
                onClick={() => navigate(targetInvoicePath)}
                className="btn btn-primary btn-lg"
                style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}
              >
                <Receipt size={18} /> Chuyển tới hóa đơn ngay ({countdown}s) <ArrowRight size={16} />
              </button>
              <Link to="/orders" className="btn btn-secondary" style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                <ShoppingBag size={16} /> Quản lý đơn hàng
              </Link>
            </>
          ) : (
            <>
              <Link
                to={result?.orderId ? `/orders/${result.orderId}` : '/orders'}
                className="btn btn-primary btn-lg"
                style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}
              >
                <ArrowLeft size={16} /> Quay lại đơn hàng để thanh toán lại
              </Link>
              <Link to="/" className="btn btn-secondary" style={{ width: '100%' }}>
                Về trang chủ
              </Link>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default PaymentResultPage;
