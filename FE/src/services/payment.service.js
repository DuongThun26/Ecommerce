import api from './api';

function generateIdempotencyKey() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return 'idemp_' + Date.now() + '_' + Math.random().toString(36).substring(2, 11);
}

export const paymentService = {
  async createVnPayUrl(orderId, idempotencyKey = null) {
    const key = idempotencyKey || generateIdempotencyKey();
    const headers = {
      'Idempotency-Key': key,
    };
    return await api.post('/payments/vnpay', { orderId }, headers);
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
