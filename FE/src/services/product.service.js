import api from './api';

export const productService = {
  async getProducts(params = {}) {
    return await api.get('/products', params);
  },

  async getProductById(id) {
    return await api.get(`/products/${id}`);
  },

  async createProduct(productData) {
    return await api.post('/products', productData);
  },

  async updateProduct(id, productData) {
    return await api.put(`/products/${id}`, productData);
  },

  async deleteProduct(id) {
    return await api.delete(`/products/${id}`);
  },
};

export default productService;
