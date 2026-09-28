import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useCart } from '../../hooks/useCart';
import {
  ShoppingBag,
  ShoppingCart,
  Receipt,
  FileText,
  Layers,
  LogOut,
  User,
  ShieldCheck,
  Menu,
  X,
  PackageCheck
} from 'lucide-react';

export const Navbar = ({ onOpenCart }) => {
  const { user, isAuthenticated, logout, isAdmin, isProductOwner } = useAuth();
  const { totalCount, openCart } = useCart();
  const navigate = useNavigate();
  const location = useLocation();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const handleOpenCart = onOpenCart || openCart;

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <header style={{
      background: 'white',
      borderBottom: '1px solid var(--border-color)',
      position: 'sticky',
      top: 0,
      zIndex: 1000,
      boxShadow: 'var(--shadow-xs)'
    }}>
      <div style={{
        maxWidth: '1280px',
        margin: '0 auto',
        padding: '0.875rem 1.5rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between'
      }}>
        {/* Brand Logo */}
        <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
          <div style={{
            background: 'linear-gradient(135deg, var(--color-primary), var(--color-primary-dark))',
            color: 'white',
            width: '38px',
            height: '38px',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 2px 6px rgba(79, 70, 229, 0.3)'
          }}>
            <ShoppingBag size={22} />
          </div>
          <div>
            <span style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--text-main)', letterSpacing: '-0.02em' }}>
              E-Commerce
            </span>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--color-primary)', marginLeft: '0.35rem', background: 'var(--color-primary-light)', padding: '0.1rem 0.4rem', borderRadius: '4px' }}>
              ENTERPRISE
            </span>
          </div>
        </Link>

        {/* Desktop Navigation Links */}
        <nav style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }} className="desktop-nav">
          <Link
            to="/"
            style={{
              fontWeight: 600,
              fontSize: '0.9rem',
              color: isActive('/') ? 'var(--color-primary)' : 'var(--text-main)',
              transition: 'color var(--transition-fast)'
            }}
          >
            Sản phẩm
          </Link>

          {/* Customer & Admin Links */}
          {isAuthenticated && !isProductOwner() && (
            <>
              <Link
                to="/orders"
                style={{
                  fontWeight: 600,
                  fontSize: '0.9rem',
                  color: isActive('/orders') ? 'var(--color-primary)' : 'var(--text-main)'
                }}
              >
                Đơn hàng của tôi
              </Link>
              <Link
                to="/invoices"
                style={{
                  fontWeight: 600,
                  fontSize: '0.9rem',
                  color: isActive('/invoices') ? 'var(--color-primary)' : 'var(--text-main)'
                }}
              >
                Hóa đơn VAT
              </Link>
            </>
          )}

          {/* Product Owner Links */}
          {isProductOwner() && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', marginLeft: '0.5rem', paddingLeft: '0.75rem', borderLeft: '1px solid var(--border-color)' }}>
              <Link
                to="/admin/tickets"
                className="btn btn-sm btn-outline"
                style={{
                  color: isActive('/admin/tickets') ? 'var(--color-primary)' : 'var(--text-main)',
                  borderColor: isActive('/admin/tickets') ? 'var(--color-primary)' : 'var(--border-color)'
                }}
              >
                <PackageCheck size={16} /> Duyệt Phiếu PO
              </Link>
              <Link
                to="/admin/products"
                className="btn btn-sm btn-outline"
                style={{
                  color: isActive('/admin/products') ? 'var(--color-primary)' : 'var(--text-main)',
                  borderColor: isActive('/admin/products') ? 'var(--color-primary)' : 'var(--border-color)'
                }}
              >
                <Layers size={16} /> Quản lý SP
              </Link>
              <Link
                to="/admin/orders"
                className="btn btn-sm btn-outline"
                style={{
                  color: isActive('/admin/orders') ? 'var(--color-primary)' : 'var(--text-main)',
                  borderColor: isActive('/admin/orders') ? 'var(--color-primary)' : 'var(--border-color)'
                }}
              >
                <FileText size={16} /> Quản lý Đơn
              </Link>
            </div>
          )}

          {/* Admin Links */}
          {isAdmin() && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginLeft: '0.5rem', paddingLeft: '0.75rem', borderLeft: '1px solid var(--border-color)' }}>
              <Link
                to="/admin/dashboard"
                className="btn btn-sm btn-primary"
              >
                <ShieldCheck size={16} /> Quản trị Admin
              </Link>
            </div>
          )}
        </nav>

        {/* User / Cart Actions */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          {/* Cart Trigger (For Customers, Admins, and Guests) */}
          {!isProductOwner() && (
            <button
              onClick={handleOpenCart}
              className="btn btn-outline btn-icon"
              style={{ position: 'relative', background: '#f8fafc' }}
              title="Giỏ hàng"
            >
              <ShoppingCart size={20} />
              {totalCount > 0 && (
                <span style={{
                  position: 'absolute',
                  top: '-6px',
                  right: '-6px',
                  background: 'var(--color-danger)',
                  color: 'white',
                  fontSize: '0.7rem',
                  fontWeight: 700,
                  width: '20px',
                  height: '20px',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: '2px solid white'
                }}>
                  {totalCount}
                </span>
              )}
            </button>
          )}

          {isAuthenticated ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', lineHeight: 1.2 }}>
                <span style={{ fontSize: '0.875rem', fontWeight: 700, color: 'var(--text-main)' }}>
                  {user?.username || 'Tài khoản'}
                </span>
                <span style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>
                  {(user?.role || user?.roles?.[0] || 'CUSTOMER').replace('ROLE_', '')}
                </span>
              </div>
              <button
                onClick={handleLogout}
                className="btn btn-sm btn-outline"
                style={{ color: 'var(--color-danger)', borderColor: 'var(--color-danger-light)' }}
                title="Đăng xuất"
              >
                <LogOut size={16} />
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <Link to="/login" className="btn btn-sm btn-outline">
                Đăng nhập
              </Link>
              <Link to="/register" className="btn btn-sm btn-primary">
                Đăng ký
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

export default Navbar;
