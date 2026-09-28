import React, { useState, useEffect } from 'react';
import Modal from '../common/Modal';

export const ProductModal = ({ isOpen, onClose, onSave, product = null, loading = false }) => {
  const [formData, setFormData] = useState({
    name: '',
    price: '',
    stock: '',
    status: 'ACTIVE',
  });

  useEffect(() => {
    if (product) {
      setFormData({
        name: product.name || '',
        price: product.price || '',
        stock: product.stock !== undefined ? product.stock : '',
        status: product.status || 'ACTIVE',
      });
    } else {
      setFormData({
        name: '',
        price: '',
        stock: '',
        status: 'ACTIVE',
      });
    }
  }, [product, isOpen]);

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave({
      name: formData.name,
      price: Number(formData.price),
      stock: Number(formData.stock),
      status: formData.status,
    });
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={product ? 'Chỉnh sửa sản phẩm' : 'Thêm sản phẩm mới'}
      footer={
        <>
          <button type="button" onClick={onClose} className="btn btn-secondary">
            Hủy
          </button>
          <button
            type="submit"
            form="product-form"
            disabled={loading || !formData.name || !formData.price}
            className="btn btn-primary"
          >
            {loading ? 'Đang lưu...' : 'Lưu sản phẩm'}
          </button>
        </>
      }
    >
      <form id="product-form" onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label">Tên sản phẩm *</label>
          <input
            type="text"
            required
            placeholder="Ví dụ: iPhone 15 Pro Max 256GB"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            className="form-control"
          />
        </div>

        <div className="form-row">
          <div className="form-group">
            <label className="form-label">Giá bán (VND) *</label>
            <input
              type="number"
              required
              min="0"
              step="1000"
              placeholder="25000000"
              value={formData.price}
              onChange={(e) => setFormData({ ...formData, price: e.target.value })}
              className="form-control"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Số lượng tồn kho *</label>
            <input
              type="number"
              required
              min="0"
              placeholder="100"
              value={formData.stock}
              onChange={(e) => setFormData({ ...formData, stock: e.target.value })}
              className="form-control"
            />
          </div>
        </div>

        <div className="form-group">
          <label className="form-label">Trạng thái kinh doanh</label>
          <select
            value={formData.status}
            onChange={(e) => setFormData({ ...formData, status: e.target.value })}
            className="form-select"
          >
            <option value="ACTIVE">ACTIVE (Đang bán)</option>
            <option value="INACTIVE">INACTIVE (Tạm ngưng)</option>
          </select>
        </div>
      </form>
    </Modal>
  );
};

export default ProductModal;
