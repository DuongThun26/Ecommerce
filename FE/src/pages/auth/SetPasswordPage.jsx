import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import authService from '../../services/auth.service';
import { useToast } from '../../hooks/useToast';
import { 
  Lock, 
  CheckCircle2, 
  AlertTriangle, 
  Eye, 
  EyeOff, 
  User, 
  Mail, 
  ArrowRight,
  ShieldCheck,
  Sparkles
} from 'lucide-react';
import LoadingSpinner from '../../components/common/LoadingSpinner';

export const SetPasswordPage = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const navigate = useNavigate();
  const { success, error } = useToast();

  const [verifying, setVerifying] = useState(true);
  const [tokenInfo, setTokenInfo] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');

  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  useEffect(() => {
    if (!token) {
      setErrorMessage('Đường dẫn không hợp lệ hoặc thiếu mã xác thực (token).');
      setVerifying(false);
      return;
    }

    const checkToken = async () => {
      setVerifying(true);
      try {
        const res = await authService.verifyActivationToken(token);
        const data = res?.data || res;
        if (data && data.valid) {
          setTokenInfo(data);
        } else {
          setErrorMessage(data?.message || 'Liên kết kích hoạt không hợp lệ hoặc đã hết hạn.');
        }
      } catch (err) {
        setErrorMessage(err.message || 'Không thể xác thực liên kết kích hoạt.');
      } finally {
        setVerifying(false);
      }
    };

    checkToken();
  }, [token]);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (password.length < 6) {
      error('Mật khẩu phải có độ dài tối thiểu 6 ký tự.');
      return;
    }

    if (password !== confirmPassword) {
      error('Mật khẩu xác nhận không trùng khớp.');
      return;
    }

    setSubmitting(true);
    try {
      await authService.setPassword(token, password);
      setIsSuccess(true);
      success('Kích hoạt tài khoản và thiết lập mật khẩu thành công!');
    } catch (err) {
      error(err.message || 'Thiết lập mật khẩu thất bại. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  if (verifying) {
    return (
      <div style={{ minHeight: '80vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <LoadingSpinner text="Đang kiểm tra liên kết kích hoạt của bạn..." />
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div style={{
        minHeight: '80vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '2rem 1rem'
      }}>
        <div className="card animate-fade-in" style={{ width: '100%', maxWidth: '460px', padding: '2.5rem 2rem', textAlign: 'center' }}>
          <div style={{
            width: '60px',
            height: '60px',
            background: '#fef2f2',
            color: '#dc2626',
            borderRadius: '50%',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: '1.25rem'
          }}>
            <AlertTriangle size={32} />
          </div>
          <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '0.75rem' }}>
            Liên kết không khả dụng
          </h2>
          <p style={{ fontSize: '0.95rem', color: 'var(--text-muted)', lineHeight: '1.6', marginBottom: '2rem' }}>
            {errorMessage}
          </p>
          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
            <Link to="/login" className="btn btn-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
              Về trang đăng nhập <ArrowRight size={16} />
            </Link>
          </div>
        </div>
      </div>
    );
  }

  if (isSuccess) {
    return (
      <div style={{
        minHeight: '80vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '2rem 1rem'
      }}>
        <div className="card animate-fade-in" style={{ width: '100%', maxWidth: '460px', padding: '2.5rem 2rem', textAlign: 'center' }}>
          <div style={{
            width: '64px',
            height: '64px',
            background: '#ecfdf5',
            color: '#10b981',
            borderRadius: '50%',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: '1.25rem'
          }}>
            <CheckCircle2 size={36} />
          </div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '0.5rem' }}>
            Kích hoạt hoàn tất!
          </h2>
          <p style={{ fontSize: '0.95rem', color: 'var(--text-muted)', lineHeight: '1.6', marginBottom: '1.5rem' }}>
            Tài khoản của bạn đã được thiết lập mật khẩu thành công. Bây giờ bạn có thể đăng nhập để sử dụng hệ thống.
          </p>

          <div style={{
            background: '#f8fafc',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
            padding: '1rem',
            marginBottom: '2rem',
            textAlign: 'left'
          }}>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-sub)', marginBottom: '0.25rem' }}>Tên đăng nhập của bạn:</div>
            <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--color-primary)', fontFamily: 'monospace' }}>
              {tokenInfo?.username}
            </div>
          </div>

          <button
            onClick={() => navigate('/login', { state: { username: tokenInfo?.username } })}
            className="btn btn-primary"
            style={{ width: '100%', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', padding: '0.85rem' }}
          >
            Đăng nhập ngay <ArrowRight size={16} />
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={{
      minHeight: '80vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '2rem 1rem'
    }}>
      <div className="card animate-fade-in" style={{ width: '100%', maxWidth: '480px', padding: '2.5rem 2rem' }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{
            width: '56px',
            height: '56px',
            background: 'var(--color-primary-light)',
            color: 'var(--color-primary)',
            borderRadius: 'var(--radius-lg)',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: '1rem'
          }}>
            <ShieldCheck size={30} />
          </div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-main)' }}>
            Thiết lập mật khẩu
          </h2>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>
            Chào mừng <strong>{tokenInfo?.name || 'quý khách'}</strong>! Vui lòng đặt mật khẩu mới để kích hoạt tài khoản.
          </p>
        </div>

        {/* Customer Account Details Card */}
        <div style={{
          background: 'linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%)',
          border: '1px solid #e2e8f0',
          borderRadius: 'var(--radius-md)',
          padding: '1rem 1.25rem',
          marginBottom: '1.75rem',
          fontSize: '0.875rem'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
            <User size={15} color="var(--text-sub)" />
            <span style={{ color: 'var(--text-sub)' }}>Tên đăng nhập:</span>
            <strong style={{ fontFamily: 'monospace', fontSize: '0.95rem', color: '#1e293b' }}>
              {tokenInfo?.username}
            </strong>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Mail size={15} color="var(--text-sub)" />
            <span style={{ color: 'var(--text-sub)' }}>Email xác nhận:</span>
            <span style={{ color: '#334155' }}>{tokenInfo?.email}</span>
          </div>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Mật khẩu mới *</label>
            <div style={{ position: 'relative' }}>
              <input
                type={showPassword ? 'text' : 'password'}
                required
                placeholder="Nhập tối thiểu 6 ký tự..."
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="form-control"
                style={{ paddingRight: '2.75rem' }}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                style={{
                  position: 'absolute',
                  right: '12px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  background: 'none',
                  border: 'none',
                  cursor: 'pointer',
                  color: 'var(--text-sub)'
                }}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          <div className="form-group" style={{ marginBottom: '1.75rem' }}>
            <label className="form-label">Xác nhận mật khẩu mới *</label>
            <input
              type={showPassword ? 'text' : 'password'}
              required
              placeholder="Nhập lại mật khẩu mới..."
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="form-control"
            />
            {confirmPassword && password !== confirmPassword && (
              <span style={{ fontSize: '0.75rem', color: '#dc2626', marginTop: '0.25rem', display: 'block' }}>
                Mật khẩu xác nhận không khớp.
              </span>
            )}
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', padding: '0.85rem' }}
            disabled={submitting || (password && confirmPassword && password !== confirmPassword)}
          >
            {submitting ? 'Đang kích hoạt...' : 'Kích hoạt tài khoản'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default SetPasswordPage;
