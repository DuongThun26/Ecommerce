import api from './api';

export const userService = {
  async getUsers() {
    return await api.get('/users');
  },

  async getUserById(id) {
    return await api.get(`/users/${id}`);
  },

  async createUser(userData) {
    return await api.post('/users', userData);
  },

  async updateRole(id, role) {
    return await api.patch(`/users/${id}/role`, { role });
  },

  async updateStatus(id, status) {
    return await api.patch(`/users/${id}/status`, { status });
  },

  async deleteUser(id) {
    return await api.delete(`/users/${id}`);
  },
};

export default userService;
