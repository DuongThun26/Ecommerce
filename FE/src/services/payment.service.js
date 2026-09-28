import api from './api';

export const paymentService = {
  async createVnPayUrl(orderId) {
    return await api.post('/payments/vnpay', { orderId });
  },

  async handleVnPayReturn(searchParams) {
    const params = Object.fromEntries(new URLSearchParams(searchParams));
    return await api.get('/payments/vnpay/return', params);
  },

  async getPaymentByOrderId(orderId) {
    return await api.get(`/payments/orders/${orderId}`);
  },

  async getPaymentById(paymentId) {
    return await api.get(`/payments/${paymentId}`);
  },
};

export default paymentService;
