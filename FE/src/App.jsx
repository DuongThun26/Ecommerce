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

                  {/* Protected Customer Routes */}
                  <Route
                    path="/checkout"
                    element={
                      <ProtectedRoute>
                        <CheckoutPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/orders"
                    element={
                      <ProtectedRoute>
                        <OrdersPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/orders/:id"
                    element={
                      <ProtectedRoute>
                        <OrderDetailPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/invoices"
                    element={
                      <ProtectedRoute>
                        <InvoicesPage />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/invoices/:id"
                    element={
                      <ProtectedRoute>
                        <InvoiceDetailPage />
                      </ProtectedRoute>
                    }
                  />

                  {/* Admin & Product Owner Routes */}
                  <Route
                    path="/admin"
                    element={
                      <ProtectedRoute roles={['ADMIN', 'PRODUCT_OWNER']}>
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
                    path="/admin/products"
                    element={
                      <ProtectedRoute roles={['ADMIN']}>
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
                      <ProtectedRoute roles={['ADMIN', 'PRODUCT_OWNER']}>
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
