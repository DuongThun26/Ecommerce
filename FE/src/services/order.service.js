import api from './api';

export const orderService = {
  async createOrder(orderData) {
    return await api.post('/orders', orderData);
  },

  async createDraftOrder(orderData) {
    return await api.post('/orders/draft', orderData);
  },

  async submitOrder(orderId) {
    return await api.post(`/orders/${orderId}/submit`, {});
  },

  async updateReturnedOrder(orderId, updateData) {
    return await api.put(`/orders/${orderId}`, updateData);
  },

  async cancelOrder(orderId, reason = '') {
    return await api.post(`/orders/${orderId}/cancel`, { reason });
  },

  async getOrders(params = {}) {
    return await api.get('/orders', params);
  },

  async getOrderById(id) {
    return await api.get(`/orders/${id}`);
  },

  async getMyOrders(params = {}) {
    return await api.get('/customers/me/orders', params);
  },

  async getCustomerOrders(customerId, params = {}) {
    if (!customerId || customerId === 'me') {
      return await api.get('/customers/me/orders', params);
    }
    return await api.get(`/customers/${customerId}/orders`, params);
  },

  async getOrderStatusHistory(orderId) {
    return await api.get(`/orders/${orderId}/history`);
  },

  async updateOrderStatus(orderId, status, reason = '') {
    return await api.patch(`/orders/${orderId}/status`, { status, reason });
  },
};

export default orderService;
