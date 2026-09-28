import React from 'react';
import { ShoppingBag, Shield, Heart } from 'lucide-react';

export const Footer = () => {
  return (
    <footer style={{
      background: 'white',
      borderTop: '1px solid var(--border-color)',
      padding: '2.5rem 1.5rem',
      marginTop: 'auto'
    }}>
      <div style={{
        maxWidth: '1280px',
        margin: '0 auto',
        display: 'flex',
        flexWrap: 'wrap',
        justifyContent: 'space-between',
        alignItems: 'center',
        gap: '1.5rem'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{
            background: 'var(--color-primary-light)',
            color: 'var(--color-primary)',
            padding: '0.5rem',
            borderRadius: 'var(--radius-md)'
          }}>
            <ShoppingBag size={20} />
          </div>
          <div>
            <div style={{ fontWeight: 800, color: 'var(--text-main)' }}>E-Commerce Enterprise</div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Hệ thống quản lý bán hàng, duyệt phiếu PO & phát hành hóa đơn điện tử</div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
            <Shield size={16} color="var(--color-success)" /> Bảo mật dữ liệu & Snapshot hóa đơn
          </span>
          <span>© 2026 E-Commerce Enterprise. All rights reserved.</span>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
