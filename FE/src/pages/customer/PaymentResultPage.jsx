import React, { useState, useEffect } from 'react';
import { useLocation, Link } from 'react-router-dom';
import paymentService from '../../services/payment.service';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { CheckCircle2, XCircle, ArrowRight, ShoppingBag, Receipt } from 'lucide-react';

export const PaymentResultPage = () => {
  const location = useLocation();
  const [loading, setLoading] = useState(true);
  const [result, setResult] = useState(null);

  useEffect(() => {
    const verifyPayment = async () => {
      setLoading(true);
      try {
        const res = await paymentService.handleVnPayReturn(location.search);
        setResult(res);
      } catch (err) {
        setResult({
          status: 'FAILED',
          message: err.message || 'Không thể xác thực giao dịch thanh toán.',
        });
      } finally {
        setLoading(false);
      }
    };

    verifyPayment();
  }, [location.search]);

  if (loading) {
    return <LoadingSpinner fullPage text="Đang xác thực kết quả thanh toán VNPay..." />;
  }

  const isSuccess = result?.status === 'SUCCESS' || result?.RspCode === '00';

  return (
    <div className="main-content" style={{ minHeight: '70vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div className="card animate-fade-in" style={{ width: '100%', maxWidth: '520px', textAlign: 'center', padding: '3rem 2rem' }}>
        <div style={{
          width: '72px',
          height: '72px',
          borderRadius: '50%',
          background: isSuccess ? 'var(--color-success-light)' : 'var(--color-danger-light)',
          color: isSuccess ? 'var(--color-success)' : 'var(--color-danger)',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: '1.5rem',
        }}>
          {isSuccess ? <CheckCircle2 size={42} /> : <XCircle size={42} />}
        </div>

        <h2 style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '0.5rem' }}>
          {isSuccess ? 'Thanh toán thành công!' : 'Thanh toán không thành công'}
        </h2>

        <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem', marginBottom: '2rem' }}>
          {result?.message || (isSuccess ? 'Đơn hàng của bạn đã được thanh toán và hóa đơn VAT đã được phát hành.' : 'Giao dịch bị gián đoạn hoặc bị hủy bởi người dùng.')}
        </p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {isSuccess ? (
            <>
              <Link to="/invoices" className="btn btn-primary btn-lg" style={{ width: '100%' }}>
                <Receipt size={18} /> Xem & Tải hóa đơn VAT
              </Link>
              <Link to="/orders" className="btn btn-secondary" style={{ width: '100%' }}>
                Quản lý đơn hàng
              </Link>
            </>
          ) : (
            <>
              <Link to="/orders" className="btn btn-primary btn-lg" style={{ width: '100%' }}>
                Quay lại đơn hàng để thanh toán lại
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
