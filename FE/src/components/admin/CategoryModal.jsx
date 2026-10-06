import React, { useState, useEffect } from 'react';
import Modal from '../common/Modal';
import userService from '../../services/user.service';
import { Layers, UserCheck } from 'lucide-react';

export const CategoryModal = ({ isOpen, onClose, onSave, category = null, loading = false, readOnly = false }) => {
  const [formData, setFormData] = useState({
    name: '',
    code: '',
    description: '',
    managedByUserId: '',
  });

  const [poUsers, setPoUsers] = useState([]);
  const [loadingUsers, setLoadingUsers] = useState(false);

  useEffect(() => {
    if (isOpen) {
      fetchPoUsers();
      if (category) {
        setFormData({
          name: category.name || '',
          code: category.code || '',
          description: category.description || '',
          managedByUserId: category.managedBy?.id || category.managedByUserId || '',
        });
      } else {
        setFormData({
          name: '',
          code: '',
          description: '',
          managedByUserId: '',
        });
      }
    }
  }, [category, isOpen]);

  const fetchPoUsers = async () => {
    setLoadingUsers(true);
    try {
      const res = await userService.getUsers();
      const usersList = Array.isArray(res) ? res : (res?.content || []);
      // Lọc các user có role PRODUCT_OWNER hoặc ADMIN
      const availablePos = usersList.filter(u => u.role === 'PRODUCT_OWNER' || u.role === 'ADMIN');
      setPoUsers(availablePos);
    } catch (err) {
      console.error('Không thể lấy danh sách PO:', err);
    } finally {
      setLoadingUsers(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (readOnly) return;
    onSave({
      name: formData.name.trim(),
      code: formData.code.trim().toUpperCase(),
      description: formData.description.trim() || undefined,
      managedByUserId: formData.managedByUserId ? Number(formData.managedByUserId) : null,
    });
  };

  const modalTitle = readOnly
    ? 'Chi tiết loại hàng'
    : category
    ? 'Chỉnh sửa loại hàng & Phân công PO'
    : 'Thêm loại hàng mới & Gán PO';

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={modalTitle}
      footer={
        <>
          <button type="button" onClick={onClose} className="btn btn-secondary" disabled={loading}>
            {readOnly ? 'Đóng' : 'Hủy'}
          </button>
          {!readOnly && (
            <button
              type="submit"
              form="category-form"
              disabled={loading || !formData.name || !formData.code}
              className="btn btn-primary"
            >
              {loading ? 'Đang lưu...' : 'Lưu loại hàng'}
            </button>
          )}
        </>
      }
    >
      <form id="category-form" onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label">Tên loại hàng *</label>
          <input
            type="text"
            required
            disabled={readOnly}
            placeholder="Ví dụ: Điện thoại thông minh & Phụ kiện"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            className="form-control"
          />
        </div>

        <div className="form-group">
          <label className="form-label">Mã loại hàng (Code duy nhất) *</label>
          <input
            type="text"
            required
            disabled={readOnly}
            placeholder="Ví dụ: SMARTPHONE"
            value={formData.code}
            onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
            className="form-control"
            style={{ textTransform: 'uppercase', fontFamily: 'monospace', fontWeight: 600 }}
          />
          <small style={{ color: 'var(--text-muted)', fontSize: '0.75rem', marginTop: '0.25rem', display: 'block' }}>
            Mã định danh viết hoa dùng trong hệ thống và phân loại sản phẩm
          </small>
        </div>

        <div className="form-group">
          <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <UserCheck size={16} color="var(--color-primary)" />
            <span>Product Owner (PO) phụ trách duyệt loại hàng này</span>
          </label>
          <select
            value={formData.managedByUserId}
            onChange={(e) => setFormData({ ...formData, managedByUserId: e.target.value })}
            className="form-select"
            disabled={readOnly || loadingUsers}
          >
            <option value="">-- Chưa chỉ định PO phụ trách --</option>
            {poUsers.map(user => (
              <option key={user.id} value={user.id}>
                {user.username} ({user.role === 'ADMIN' ? 'Admin' : 'Product Owner'}) - {user.email || 'No email'}
              </option>
            ))}
          </select>
          <small style={{ color: 'var(--text-muted)', fontSize: '0.75rem', marginTop: '0.25rem', display: 'block' }}>
            PO được chỉ định sẽ có quyền thêm/sửa sản phẩm và phê duyệt các đơn hàng thuộc loại hàng này
          </small>
        </div>

        <div className="form-group">
          <label className="form-label">Mô tả loại hàng</label>
          <textarea
            rows={3}
            disabled={readOnly}
            placeholder="Mô tả về nhóm sản phẩm, tiêu chuẩn hoặc ghi chú quản trị..."
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            className="form-control"
          />
        </div>
      </form>
    </Modal>
  );
};

export default CategoryModal;
