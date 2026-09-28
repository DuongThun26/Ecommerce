import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import LoadingSpinner from './LoadingSpinner';

export const ProtectedRoute = ({ children, roles = [], allowedRoles = [] }) => {
  const { user, token, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <LoadingSpinner fullPage text="Đang xác thực tài khoản..." />;
  }

  if (!token || !user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  const requiredRoles = roles.length > 0 ? roles : allowedRoles;
  if (requiredRoles.length > 0) {
    const rawRoles = [
      user.role,
      ...(Array.isArray(user.roles) ? user.roles : user.roles ? [user.roles] : []),
    ].filter(Boolean);

    const normalizedUserRoles = rawRoles.map((r) =>
      typeof r === 'string' ? r.replace('ROLE_', '').toUpperCase() : ''
    );

    const isAllowed = requiredRoles.some((reqRole) =>
      normalizedUserRoles.includes(String(reqRole).replace('ROLE_', '').toUpperCase())
    );

    if (!isAllowed) {
      return <Navigate to="/" replace />;
    }
  }

  return children ? children : <Outlet />;
};

export default ProtectedRoute;
