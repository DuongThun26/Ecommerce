import api from './api';

export const invoiceService = {
  async getInvoices(params = {}) {
    return await api.get('/invoices', params);
  },

  async getInvoiceById(id) {
    return await api.get(`/invoices/${id}`);
  },

  async getInvoiceByNumber(invoiceNumber) {
    return await api.get(`/invoices/by-number/${invoiceNumber}`);
  },

  async getInvoiceByOrderId(orderId) {
    return await api.get(`/orders/${orderId}/invoice`);
  },

  async getMyInvoices(params = {}) {
    return await api.get('/customers/me/invoices', params);
  },

  async getCustomerInvoices(customerId, params = {}) {
    if (!customerId || customerId === 'me') {
      return await api.get('/customers/me/invoices', params);
    }
    return await api.get(`/customers/${customerId}/invoices`, params);
  },

  async downloadInvoicePdf(invoiceId, invoiceNumber = 'invoice') {
    return await api.download(`/invoices/${invoiceId}/pdf`, `${invoiceNumber}.pdf`);
  },

  async generateInvoiceForOrder(orderId) {
    return await api.post(`/invoices/orders/${orderId}/generate`, {});
  },
};

export default invoiceService;
