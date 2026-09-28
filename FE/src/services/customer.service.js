import api from './api';

export const customerService = {
  async getCustomers() {
    return await api.get('/customers');
  },

  async getCustomerById(id) {
    return await api.get(`/customers/${id}`);
  },

  async createCustomer(data) {
    return await api.post('/customers', data);
  },

  async updateCustomer(id, data) {
    return await api.put(`/customers/${id}`, data);
  },

  async deleteCustomer(id) {
    return await api.delete(`/customers/${id}`);
  },
};

export default customerService;
