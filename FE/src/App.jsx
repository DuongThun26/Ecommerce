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
import CheckoutPage from './pages/customer/CheckoutPage';
import OrdersPage from './pages/customer/OrdersPage';
import OrderDetailPage from './pages/customer/OrderDetailPage';
import InvoicesPage from './pages/customer/InvoicesPage';
import InvoiceDetailPage from './pages/customer/InvoiceDetailPage';
import PaymentResultPage from './pages/customer/PaymentResultPage';

// Admin / PO Pages
import DashboardPage from './pages/admin/DashboardPage';
import TicketManagementPage from './pages/admin/TicketManagementPage';
import ProductManagementPage from './pages/admin/ProductManagementPage';
import OrderManagementPage from './pages/admin/OrderManagementPage';
import InvoiceManagementPage from './pages/admin/InvoiceManagementPage';

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
                  <Route path="/login" element={<LoginPage />} />
                  <Route path="/register" element={<RegisterPage />} />
                  <Route path="/payment/vnpay-return" element={<PaymentResultPage />} />

                  {/* Protected Customer Routes (Only CUSTOMER) */}
                  <Route
                    path="/checkout"
                    element={
                      <ProtectedRoute roles={['CUSTOMER']}>
                        <CheckoutPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/orders"
                    element={
                      <ProtectedRoute roles={['CUSTOMER']}>
                        <OrdersPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/orders/:id"
                    element={
                      <ProtectedRoute roles={['CUSTOMER']}>
                        <OrderDetailPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/invoices"
                    element={
                      <ProtectedRoute roles={['CUSTOMER']}>
                        <InvoicesPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/invoices/:id"
                    element={
                      <ProtectedRoute roles={['CUSTOMER']}>
                        <InvoiceDetailPage />
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
