import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { ToastProvider } from './context/ToastContext';

// Common Components
import Navbar from './components/common/Navbar';
import Footer from './components/common/Footer';
import CartDrawer from './components/customer/CartDrawer';
import ToastContainer from './components/common/ToastContainer';
import ProtectedRoute from './components/common/ProtectedRoute';

// Auth Pages
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';

// Customer Pages
import HomePage from './pages/customer/HomePage';
import ProductDetailPage from './pages/customer/ProductDetailPage';
import CheckoutPage from './pages/customer/CheckoutPage';
import OrdersPage from './pages/customer/OrdersPage';
import OrderDetailPage from './pages/customer/OrderDetailPage';
import InvoicesPage from './pages/customer/InvoicesPage';
import InvoiceDetailPage from './pages/customer/InvoiceDetailPage';
import PaymentResultPage from './pages/customer/PaymentResultPage';
import ProfilePage from './pages/customer/ProfilePage';

// Admin / PO Pages
import DashboardPage from './pages/admin/DashboardPage';
import TicketManagementPage from './pages/admin/TicketManagementPage';
import CategoryManagementPage from './pages/admin/CategoryManagementPage';
import ProductManagementPage from './pages/admin/ProductManagementPage';
import OrderManagementPage from './pages/admin/OrderManagementPage';
import InvoiceManagementPage from './pages/admin/InvoiceManagementPage';
import UserManagementPage from './pages/admin/UserManagementPage';
import CustomerManagementPage from './pages/admin/CustomerManagementPage';

// 404 Page
import NotFoundPage from './pages/NotFoundPage';
import { useAuth } from './hooks/useAuth';

const AdminIndexRedirect = () => {
  const { isProductOwner } = useAuth();
  if (isProductOwner()) {
    return <Navigate to="/admin/tickets" replace />;
  }
  return <Navigate to="/admin/dashboard" replace />;
};

export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <CartProvider>
            <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
              <Navbar />
              <CartDrawer />
              <ToastContainer />
              
              <main style={{ flex: 1 }}>
                <Routes>
                  {/* Public Customer Routes */}
                  <Route path="/" element={<HomePage />} />
                  <Route path="/products/:id" element={<ProductDetailPage />} />
                  <Route path="/login" element={<LoginPage />} />
                  <Route path="/register" element={<RegisterPage />} />
                  <Route path="/payment/vnpay-return" element={<PaymentResultPage />} />

                  {/* Protected Routes (CUSTOMER & ADMIN can create/view orders & invoices) */}
                  <Route
                    path="/checkout"
                    element={
                      <ProtectedRoute roles={['CUSTOMER', 'ADMIN']}>
                        <CheckoutPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/orders"
                    element={
                      <ProtectedRoute roles={['CUSTOMER', 'ADMIN']}>
                        <OrdersPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/orders/:id"
                    element={
                      <ProtectedRoute roles={['CUSTOMER', 'ADMIN', 'PRODUCT_OWNER']}>
                        <OrderDetailPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/invoices"
                    element={
                      <ProtectedRoute roles={['CUSTOMER', 'ADMIN']}>
                        <InvoicesPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/invoices/:id"
                    element={
                      <ProtectedRoute roles={['CUSTOMER', 'ADMIN']}>
                        <InvoiceDetailPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/profile"
                    element={
                      <ProtectedRoute roles={['CUSTOMER', 'ADMIN', 'PRODUCT_OWNER']}>
                        <ProfilePage />
                      </ProtectedRoute>
                    }
                  />

                  {/* Admin & Product Owner Routes */}
                  <Route
                    path="/admin"
                    element={
                      <ProtectedRoute roles={['ADMIN', 'PRODUCT_OWNER']}>
                        <AdminIndexRedirect />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/admin/dashboard"
                    element={
                      <ProtectedRoute roles={['ADMIN']}>
                        <DashboardPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/admin/tickets"
                    element={
                      <ProtectedRoute roles={['ADMIN', 'PRODUCT_OWNER']}>
                        <TicketManagementPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/tickets"
                    element={
                      <ProtectedRoute roles={['ADMIN', 'PRODUCT_OWNER']}>
                        <TicketManagementPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/po/tickets"
                    element={
                      <ProtectedRoute roles={['ADMIN', 'PRODUCT_OWNER']}>
                        <TicketManagementPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/admin/categories"
                    element={
                      <ProtectedRoute roles={['ADMIN', 'PRODUCT_OWNER']}>
                        <CategoryManagementPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/admin/products"
                    element={
                      <ProtectedRoute roles={['ADMIN', 'PRODUCT_OWNER']}>
                        <ProductManagementPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/admin/orders"
                    element={
                      <ProtectedRoute roles={['ADMIN', 'PRODUCT_OWNER']}>
                        <OrderManagementPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/admin/invoices"
                    element={
                      <ProtectedRoute roles={['ADMIN']}>
                        <InvoiceManagementPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/admin/customers"
                    element={
                      <ProtectedRoute roles={['ADMIN']}>
                        <CustomerManagementPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/admin/users"
                    element={
                      <ProtectedRoute roles={['ADMIN']}>
                        <UserManagementPage />
                      </ProtectedRoute>
                    }
                  />

                  {/* Fallback 404 */}
                  <Route path="/404" element={<NotFoundPage />} />
                  <Route path="*" element={<Navigate to="/404" replace />} />
                </Routes>
              </main>

              <Footer />
            </div>
          </CartProvider>
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}
