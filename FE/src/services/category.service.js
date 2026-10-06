import api from './api';

export const categoryService = {
  async getCategories(params = {}) {
    return await api.get('/categories', params);
  },

  async getCategoryById(id) {
    return await api.get(`/categories/${id}`);
  },

  async getCategoryByCode(code) {
    return await api.get(`/categories/code/${code}`);
  },

  async getMyManagedCategories() {
    return await api.get('/categories/my-categories');
  },

  async createCategory(categoryData) {
    return await api.post('/categories', categoryData);
  },

  async updateCategory(id, categoryData) {
    return await api.put(`/categories/${id}`, categoryData);
  },

  async deleteCategory(id) {
    return await api.delete(`/categories/${id}`);
  },
};

export default categoryService;
