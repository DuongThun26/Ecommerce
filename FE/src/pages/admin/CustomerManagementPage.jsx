import React, { useState, useEffect } from 'react';
import Sidebar from '../../components/admin/Sidebar';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Badge from '../../components/common/Badge';
import customerService from '../../services/customer.service';
import { useToast } from '../../hooks/useToast';
import {
  Users,
  Search,
  Plus,
  Edit2,
  Trash2,
  RotateCcw,
  Mail,
  Phone,
  Send,
  CheckCircle2,
  X,
  ShieldAlert,
  Info,
  Clock
} from 'lucide-react';

export const CustomerManagementPage = () => {
  const { success, error } = useToast();
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');

  // Modal State for Create / Edit
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    status: 'ACTIVE',
  });
  const [modalLoading, setModalLoading] = useState(false);
  const [resendingId, setResendingId] = useState(null);

  const fetchCustomers = async () => {
    setLoading(true);
    try {
      const res = await customerService.getCustomers();
      if (Array.isArray(res)) {
        setCustomers(res);
      } else if (res && res.content) {
        setCustomers(res.content);
      } else if (res && res.data) {
        setCustomers(Array.isArray(res.data) ? res.data : res.data.content || []);
      } else {
        setCustomers([]);
      }
    } catch (err) {
      error(err.message || 'Không thể tải danh sách khách hàng.');
      setCustomers([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCustomers();
  }, []);

  const handleOpenCreate = () => {
    setSelectedCustomer(null);
    setFormData({
      name: '',
      email: '',
      phone: '',
      status: 'INACTIVE',
    });
    setIsModalOpen(true);
  };

  const handleOpenEdit = (customer) => {
    setSelectedCustomer(customer);
    setFormData({
      name: customer.name || '',
      email: customer.email || '',
      phone: customer.phone || '',
      status: customer.status || 'ACTIVE',
    });
    setIsModalOpen(true);
  };

  const handleSaveCustomer = async (e) => {
    e.preventDefault();
    setModalLoading(true);
    try {
      if (selectedCustomer) {
        await customerService.updateCustomer(selectedCustomer.id, formData);
        success('Cập nhật thông tin khách hàng thành công!');
      } else {
        await customerService.createCustomer(formData);
        success('Tạo khách hàng thành công! Email kèm liên kết kích hoạt đã được gửi tới khách hàng.');
      }
      setIsModalOpen(false);
      fetchCustomers();
    } catch (err) {
      error(err.message || 'Lỗi khi lưu thông tin khách hàng.');
    } finally {
      setModalLoading(false);
    }
  };

  const handleDeleteCustomer = async (id) => {
    if (!window.confirm('Bạn có chắc muốn xóa thông tin khách hàng này?')) return;
    try {
      await customerService.deleteCustomer(id);
      success('Đã xóa khách hàng thành công!');
      fetchCustomers();
    } catch (err) {
      error(err.message || 'Lỗi khi xóa khách hàng.');
    }
  };

  const handleResendActivation = async (id) => {
    setResendingId(id);
    try {
      await customerService.resendActivation(id);
      success('Đã gửi lại email kích hoạt tài khoản thành công!');
    } catch (err) {
      error(err.message || 'Không thể gửi lại email kích hoạt.');
    } finally {
      setResendingId(null);
    }
  };

  const filteredCustomers = customers.filter((c) => {
    if (!keyword) return true;
    const kw = keyword.toLowerCase();
    return (
      (c.name && c.name.toLowerCase().includes(kw)) ||
      (c.email && c.email.toLowerCase().includes(kw)) ||
      (c.username && c.username.toLowerCase().includes(kw)) ||
      (c.phone && c.phone.includes(kw))
    );
  });

  return (
    <div style={{ display: 'flex' }}>
      <Sidebar />

      <main style={{ flex: 1, padding: '2rem' }} className="animate-fade-in">
        <div className="page-header">
          <div>
            <h1 className="page-title">Quản lý Khách hàng</h1>
            <p className="page-subtitle">Danh sách khách hàng, tài khoản đăng nhập và trạng thái kích hoạt</p>
          </div>

          <button onClick={handleOpenCreate} className="btn btn-primary">
            <Plus size={18} /> Thêm khách hàng mới
          </button>
        </div>

        {/* Filter Bar */}
        <div className="card" style={{ padding: '1.25rem', marginBottom: '1.5rem', background: '#f8fafc', border: '1px solid var(--border-color)' }}>
          <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
            <div style={{ flex: 1, position: 'relative' }}>
              <Search size={18} style={{ position: 'absolute', left: '12px', top: '10px', color: 'var(--text-sub)' }} />
              <input
                type="text"
                placeholder="Tìm theo họ tên, username, email, số điện thoại..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                className="form-control"
                style={{ paddingLeft: '2.5rem' }}
              />
            </div>
            {keyword && (
              <button
                type="button"
                onClick={() => setKeyword('')}
                className="btn btn-secondary"
                title="Đặt lại tìm kiếm"
              >
                <RotateCcw size={16} /> Đặt lại
              </button>
            )}
          </div>
        </div>

        {/* Table */}
        {loading ? (
          <LoadingSpinner text="Đang tải danh sách khách hàng..." />
        ) : filteredCustomers.length === 0 ? (
          <div style={{
            textAlign: 'center',
            padding: '4rem 2rem',
            background: 'white',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border-color)',
            color: 'var(--text-muted)',
          }}>
            <Users size={48} style={{ margin: '0 auto 1rem auto', opacity: 0.6 }} />
            <p style={{ fontSize: '1.1rem', fontWeight: 600 }}>Không tìm thấy khách hàng nào.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Họ và tên</th>
                  <th>Tên đăng nhập</th>
                  <th>Email</th>
                  <th>Số điện thoại</th>
                  <th>Trạng thái tài khoản</th>
                  <th style={{ textAlign: 'right' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {filteredCustomers.map((c) => {
                  const isPending = c.userStatus === 'PENDING_ACTIVATION';
                  const isActive = c.userStatus === 'ACTIVE' || (!c.userStatus && c.status === 'ACTIVE');

                  return (
                    <tr key={c.id}>
                      <td>#{c.id}</td>
                      <td><strong>{c.name}</strong></td>
                      <td>
                        {c.username ? (
                          <span style={{
                            fontFamily: 'monospace',
                            fontWeight: 600,
                            color: '#1d4ed8',
                            background: '#eff6ff',
                            padding: '3px 8px',
                            borderRadius: '4px',
                            border: '1px solid #dbeafe',
                            fontSize: '0.875rem'
                          }}>
                            {c.username}
                          </span>
                        ) : (
                          <span style={{ color: 'var(--text-muted)' }}>-</span>
                        )}
                      </td>
                      <td>
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
                          <Mail size={14} color="var(--text-sub)" /> {c.email || '-'}
                        </span>
                      </td>
                      <td>
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
                          <Phone size={14} color="var(--text-sub)" /> {c.phone || '-'}
                        </span>
                      </td>
                      <td>
                        {isPending ? (
                          <Badge variant="warning">
                            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                              <Clock size={12} /> Chờ kích hoạt
                            </span>
                          </Badge>
                        ) : isActive ? (
                          <Badge variant="success">Hoạt động</Badge>
                        ) : (
                          <Badge variant="neutral">Tạm khóa</Badge>
                        )}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: '0.5rem', alignItems: 'center' }}>
                          {isPending && (
                            <button
                              onClick={() => handleResendActivation(c.id)}
                              className="btn btn-sm btn-outline"
                              style={{ color: '#b45309', borderColor: '#fde68a', background: '#fffbeb' }}
                              title="Gửi lại link kích hoạt tài khoản qua email"
                              disabled={resendingId === c.id}
                            >
                              <Send size={13} /> {resendingId === c.id ? 'Đang gửi...' : 'Gửi lại link'}
                            </button>
                          )}
                          <button
                            onClick={() => handleOpenEdit(c)}
                            className="btn btn-sm btn-outline"
                            title="Sửa thông tin"
                          >
                            <Edit2 size={14} /> Sửa
                          </button>
                          <button
                            onClick={() => handleDeleteCustomer(c.id)}
                            className="btn btn-sm btn-danger"
                            title="Xóa khách hàng"
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Modal Create / Edit */}
        {isModalOpen && (
          <div
            style={{
              position: 'fixed',
              inset: 0,
              backgroundColor: 'rgba(15, 23, 42, 0.5)',
              backdropFilter: 'blur(3px)',
              zIndex: 2000,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '1rem',
            }}
            onClick={() => setIsModalOpen(false)}
          >
            <div
              className="card animate-fade-in"
              style={{
                width: '100%',
                maxWidth: '520px',
                padding: '2rem',
                background: 'white',
                boxShadow: 'var(--shadow-xl)',
              }}
              onClick={(e) => e.stopPropagation()}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                <h3 style={{ fontSize: '1.25rem', fontWeight: 800 }}>
                  {selectedCustomer ? 'Cập nhật thông tin khách hàng' : 'Thêm khách hàng mới'}
                </h3>
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-sub)' }}
                >
                  <X size={20} />
                </button>
              </div>

              {!selectedCustomer && (
                <div style={{
                  display: 'flex',
                  gap: '0.75rem',
                  padding: '0.85rem 1rem',
                  background: '#eff6ff',
                  border: '1px solid #bfdbfe',
                  borderRadius: 'var(--radius-md)',
                  marginBottom: '1.5rem',
                  fontSize: '0.85rem',
                  color: '#1e40af',
                  lineHeight: '1.5'
                }}>
                  <Info size={20} style={{ flexShrink: 0, marginTop: '2px' }} />
                  <div>
                    Hệ thống sẽ <strong>tự động sinh tên đăng nhập</strong> và gửi <strong>email kích hoạt kèm liên kết</strong> (có hiệu lực 24h) để khách hàng tự thiết lập mật khẩu của mình.
                  </div>
                </div>
              )}

              <form onSubmit={handleSaveCustomer}>
                <div className="form-group">
                  <label className="form-label">Họ và tên *</label>
                  <input
                    type="text"
                    required
                    placeholder="Nguyễn Văn A"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="form-control"
                  />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">Email *</label>
                    <input
                      type="email"
                      required
                      placeholder="vana@example.com"
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                      className="form-control"
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Số điện thoại *</label>
                    <input
                      type="tel"
                      required
                      placeholder="0901234567"
                      value={formData.phone}
                      onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                      className="form-control"
                    />
                  </div>
                </div>

                {selectedCustomer && (
                  <div className="form-group">
                    <label className="form-label">Trạng thái hồ sơ</label>
                    <select
                      value={formData.status}
                      onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                      className="form-select"
                    >
                      <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                      <option value="INACTIVE">Tạm khóa (INACTIVE)</option>
                    </select>
                  </div>
                )}

                <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '2rem' }}>
                  <button
                    type="button"
                    onClick={() => setIsModalOpen(false)}
                    className="btn btn-secondary"
                    disabled={modalLoading}
                  >
                    Hủy bỏ
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={modalLoading}
                  >
                    <CheckCircle2 size={16} /> {modalLoading ? 'Đang lưu...' : selectedCustomer ? 'Cập nhật' : 'Tạo khách hàng & Gửi email'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};

export default CustomerManagementPage;
