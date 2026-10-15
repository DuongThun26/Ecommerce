import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import {
  LayoutDashboard,
  Package,
  PackageCheck,
  ShoppingBag,
  Receipt,
  Users,
  User,
  UserCog,
  ArrowLeft,
  Layers
} from 'lucide-react';

export const Sidebar = () => {
  const { isAdmin, isProductOwner } = useAuth();

  const links = [
    { to: '/admin/dashboard', icon: LayoutDashboard, label: 'Tổng quan', visible: isAdmin() },
    { to: '/admin/tickets', icon: PackageCheck, label: 'Duyệt Phiếu Mua Hàng', visible: isProductOwner() || isAdmin() },
    {
      to: '/admin/categories',
      icon: Layers,
      label: isAdmin() ? 'Loại hàng & Phân công PO' : 'Loại hàng phụ trách',
      visible: isAdmin() || isProductOwner(),
    },
    { to: '/admin/products', icon: Package, label: 'Quản lý Sản phẩm', visible: isProductOwner() || isAdmin() },
    { to: '/admin/orders', icon: ShoppingBag, label: 'Quản lý Đơn hàng', visible: isProductOwner() || isAdmin() },
    { to: '/admin/invoices', icon: Receipt, label: 'Quản lý Hóa đơn', visible: isAdmin() },
    { to: '/admin/customers', icon: Users, label: 'Quản lý Khách hàng', visible: isAdmin() },
    { to: '/admin/users', icon: UserCog, label: 'Người dùng & Phân quyền', visible: isAdmin() },
    { to: '/profile', icon: User, label: 'Hồ sơ cá nhân', visible: true },
  ];

  return (
    <aside style={{
      width: '260px',
      background: 'white',
      borderRight: '1px solid var(--border-color)',
      minHeight: 'calc(100vh - 65px)',
      padding: '1.5rem 1rem',
      display: 'flex',
      flexDirection: 'column',
      gap: '0.5rem',
    }}>
      <div style={{ padding: '0 0.5rem 1rem 0.5rem', borderBottom: '1px solid var(--border-color)', marginBottom: '0.5rem' }}>
        <h4 style={{ fontSize: '0.8rem', fontWeight: 800, textTransform: 'uppercase', color: 'var(--text-muted)', letterSpacing: '0.05em' }}>
          Quản trị hệ thống
        </h4>
      </div>

      <nav style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', flex: 1 }}>
        {links.filter(l => l.visible).map((link) => {
          const Icon = link.icon;
          return (
            <NavLink
              key={link.to}
              to={link.to}
              style={({ isActive }) => ({
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                padding: '0.75rem 1rem',
                borderRadius: 'var(--radius-md)',
                fontWeight: 600,
                fontSize: '0.9rem',
                color: isActive ? 'var(--color-primary)' : 'var(--text-main)',
                backgroundColor: isActive ? 'var(--color-primary-light)' : 'transparent',
                transition: 'all var(--transition-fast)',
              })}
            >
              <Icon size={18} />
              <span>{link.label}</span>
            </NavLink>
          );
        })}
      </nav>

      <div style={{ paddingTop: '1rem', borderTop: '1px solid var(--border-color)' }}>
        <NavLink
          to="/"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.625rem 1rem',
            borderRadius: 'var(--radius-md)',
            fontSize: '0.85rem',
            fontWeight: 600,
            color: 'var(--text-muted)',
          }}
        >
          <ArrowLeft size={16} /> Quay về Cửa hàng
        </NavLink>
      </div>
    </aside>
  );
};

export default Sidebar;
