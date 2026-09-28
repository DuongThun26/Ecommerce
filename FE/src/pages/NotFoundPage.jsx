import React from 'react';
import { Link } from 'react-router-dom';
import { AlertCircle, Home, ShoppingBag } from 'lucide-react';

export default function NotFoundPage() {
  return (
    <div style={{
      minHeight: '60vh',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      textAlign: 'center',
      padding: '40px 20px'
    }}>
      <div style={{
        width: '80px',
        height: '80px',
        borderRadius: '50%',
        backgroundColor: '#fee2e2',
        color: '#dc2626',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: '24px'
      }}>
        <AlertCircle size={48} />
      </div>
      <h1 style={{ fontSize: '3rem', fontWeight: 800, margin: '0 0 8px 0', color: '#1e293b' }}>404</h1>
      <h2 style={{ fontSize: '1.5rem', fontWeight: 600, margin: '0 0 16px 0', color: '#475569' }}>
        Trang không tìm thấy
      </h2>
      <p style={{ color: '#64748b', maxWidth: '480px', marginBottom: '32px', lineHeight: 1.6 }}>
        Đường dẫn bạn truy cập không tồn tại hoặc đã được di chuyển sang địa chỉ khác.
      </p>
      <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', justifyContent: 'center' }}>
        <Link to="/" className="btn btn-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
          <Home size={18} /> Về trang chủ
        </Link>
        <Link to="/" className="btn btn-outline" style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
          <ShoppingBag size={18} /> Mua sắm
        </Link>
      </div>
    </div>
  );
}
