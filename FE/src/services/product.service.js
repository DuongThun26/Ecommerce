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

  // Upload single file directly to Cloudinary
  async uploadImage(file) {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.upload('/cloudinary/upload', formData);
    return res?.url || res?.data?.url || res;
  },

  // Upload 1 or multiple files to Cloudinary
  async uploadMultipleImages(files) {
    if (!files || files.length === 0) return [];
    const formData = new FormData();
    for (let i = 0; i < files.length; i++) {
      formData.append('files', files[i]);
    }
    const res = await api.upload('/cloudinary/upload-multiple', formData);
    return res?.urls || res?.data?.urls || [];
  },

  // Add more images to an existing product
  async addProductImages(productId, files) {
    const formData = new FormData();
    for (let i = 0; i < files.length; i++) {
      formData.append('files', files[i]);
    }
    return await api.upload(`/products/${productId}/images`, formData);
  },

  // Delete a specific product image
  async deleteProductImage(productId, imageId) {
    return await api.delete(`/products/${productId}/images/${imageId}`);
  },
};

export default productService;
