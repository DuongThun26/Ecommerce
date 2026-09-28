import React, { useState, useEffect } from 'react';
import Sidebar from '../../components/admin/Sidebar';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Badge from '../../components/common/Badge';
import userService from '../../services/user.service';
import { formatDateTime } from '../../utils/formatters';
import { useToast } from '../../hooks/useToast';
import {
  Users,
  ShieldCheck,
  Search,
  UserCheck,
  UserX,
  Edit,
  Key,
  RotateCcw,
  CheckCircle2,
  XCircle,
  Lock
} from 'lucide-react';

const ROLE_BADGE = {
  ADMIN: 'danger',
  PRODUCT_OWNER: 'warning',
  CUSTOMER: 'primary',
};

const ROLE_LABELS = {
  ADMIN: 'Quản trị viên (ADMIN)',
  PRODUCT_OWNER: 'Product Owner (PO)',
  CUSTOMER: 'Khách hàng (CUSTOMER)',
};

const STATUS_BADGE = {
  ACTIVE: 'success',
  INACTIVE: 'neutral',
};

export const UserManagementPage = () => {
  const { success, error } = useToast();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  // Role Assignment Modal State
  const [selectedUser, setSelectedUser] = useState(null);
  const [newRole, setNewRole] = useState('');
  const [isRoleModalOpen, setIsRoleModalOpen] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await userService.getUsers();
      if (Array.isArray(res)) {
        setUsers(res);
      } else if (res && res.content) {
        setUsers(res.content);
      } else if (res && res.data) {
        setUsers(Array.isArray(res.data) ? res.data : res.data.content || []);
      } else {
        setUsers([]);
      }
    } catch (err) {
      error(err.message || 'Không thể tải danh sách người dùng.');
      setUsers([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleOpenRoleModal = (user) => {
    setSelectedUser(user);
    setNewRole(user.role || 'CUSTOMER');
    setIsRoleModalOpen(true);
  };

  const handleSaveRole = async () => {
    if (!selectedUser || !newRole) return;
    setActionLoading(true);
    try {
      await userService.updateRole(selectedUser.id, newRole);
      success(`Cập nhật quyền thành công cho người dùng "${selectedUser.username}" thành ${newRole}!`);
      setIsRoleModalOpen(false);
      fetchUsers();
    } catch (err) {
      error(err.message || 'Lỗi khi cập nhật vai trò người dùng.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleToggleStatus = async (user) => {
    const nextStatus = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const actionText = nextStatus === 'ACTIVE' ? 'kích hoạt' : 'khóa';
    if (!window.confirm(`Bạn có chắc muốn ${actionText} tài khoản "${user.username}"?`)) return;

    try {
      await userService.updateStatus(user.id, nextStatus);
      success(`Đã ${actionText} tài khoản "${user.username}" thành công!`);
      fetchUsers();
    } catch (err) {
      error(err.message || 'Lỗi khi thay đổi trạng thái người dùng.');
    }
  };

  const filteredUsers = users.filter((u) => {
    const matchesKeyword = !keyword ||
      (u.username && u.username.toLowerCase().includes(keyword.toLowerCase())) ||
      (u.email && u.email.toLowerCase().includes(keyword.toLowerCase()));
    const matchesRole = !roleFilter || u.role === roleFilter;
    const matchesStatus = !statusFilter || u.status === statusFilter;
    return matchesKeyword && matchesRole && matchesStatus;
  });

  return (
    <div style={{ display: 'flex' }}>
      <Sidebar />

      <main style={{ flex: 1, padding: '2rem' }} className="animate-fade-in">
        <div className="page-header">
          <div>
            <h1 className="page-title">Quản lý Người dùng & Phân quyền</h1>
            <p className="page-subtitle">Danh sách tài khoản hệ thống, phân quyền vai trò (Admin, Product Owner, Customer) và trạng thái hoạt động</p>
          </div>
        </div>

        {/* Filter Bar */}
        <div className="card" style={{ padding: '1.25rem', marginBottom: '1.5rem', background: '#f8fafc', border: '1px solid var(--border-color)' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', alignItems: 'flex-end' }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label" style={{ fontSize: '0.8rem' }}>Tìm kiếm</label>
              <div style={{ position: 'relative' }}>
                <Search size={18} style={{ position: 'absolute', left: '12px', top: '10px', color: 'var(--text-sub)' }} />
                <input
                  type="text"
                  placeholder="Tìm username, email..."
                  value={keyword}
                  onChange={(e) => setKeyword(e.target.value)}
                  className="form-control"
                  style={{ paddingLeft: '2.5rem' }}
                />
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label" style={{ fontSize: '0.8rem' }}>Vai trò (Role)</label>
              <select
                value={roleFilter}
                onChange={(e) => setRoleFilter(e.target.value)}
                className="form-select"
              >
                <option value="">Tất cả vai trò</option>
                <option value="ADMIN">ADMIN</option>
                <option value="PRODUCT_OWNER">PRODUCT_OWNER</option>
                <option value="CUSTOMER">CUSTOMER</option>
              </select>
            </div>

            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label" style={{ fontSize: '0.8rem' }}>Trạng thái</label>
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="form-select"
              >
                <option value="">Tất cả trạng thái</option>
                <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                <option value="INACTIVE">Đã khóa (INACTIVE)</option>
              </select>
            </div>

            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button
                type="button"
                onClick={() => { setKeyword(''); setRoleFilter(''); setStatusFilter(''); }}
                className="btn btn-secondary"
                title="Đặt lại bộ lọc"
                style={{ width: '100%' }}
              >
                <RotateCcw size={16} /> Đặt lại
              </button>
            </div>
          </div>
        </div>

        {/* Users Table */}
        {loading ? (
          <LoadingSpinner text="Đang tải danh sách người dùng..." />
        ) : filteredUsers.length === 0 ? (
          <div style={{
            textAlign: 'center',
            padding: '4rem 2rem',
            background: 'white',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border-color)',
            color: 'var(--text-muted)',
          }}>
            <Users size={48} style={{ margin: '0 auto 1rem auto', opacity: 0.6 }} />
            <p style={{ fontSize: '1.1rem', fontWeight: 600 }}>Không tìm thấy người dùng nào.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Vai trò (Role)</th>
                  <th>Trạng thái</th>
                  <th>Ngày tạo</th>
                  <th style={{ textAlign: 'right' }}>Thao tác phân quyền</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((u) => (
                  <tr key={u.id}>
                    <td>#{u.id}</td>
                    <td><strong>{u.username}</strong></td>
                    <td>{u.email || '-'}</td>
                    <td>
                      <Badge variant={ROLE_BADGE[u.role] || 'neutral'}>
                        {u.role || 'CUSTOMER'}
                      </Badge>
                    </td>
                    <td>
                      <Badge variant={STATUS_BADGE[u.status] || 'neutral'}>
                        {u.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm khóa'}
                      </Badge>
                    </td>
                    <td>{formatDateTime(u.createdAt)}</td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '0.5rem' }}>
                        <button
                          onClick={() => handleOpenRoleModal(u)}
                          className="btn btn-sm btn-primary"
                          title="Phân quyền vai trò"
                        >
                          <ShieldCheck size={14} /> Phân quyền
                        </button>
                        <button
                          onClick={() => handleToggleStatus(u)}
                          className={`btn btn-sm ${u.status === 'ACTIVE' ? 'btn-danger' : 'btn-outline'}`}
                          title={u.status === 'ACTIVE' ? 'Khóa tài khoản' : 'Mở khóa'}
                        >
                          {u.status === 'ACTIVE' ? <UserX size={14} /> : <UserCheck size={14} />}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Role Assignment Modal */}
        {isRoleModalOpen && (
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
            onClick={() => setIsRoleModalOpen(false)}
          >
            <div
              className="card animate-fade-in"
              style={{
                width: '100%',
                maxWidth: '480px',
                padding: '2rem',
                background: 'white',
                boxShadow: 'var(--shadow-xl)',
              }}
              onClick={(e) => e.stopPropagation()}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.25rem' }}>
                <ShieldCheck size={24} color="var(--color-primary)" />
                <h3 style={{ fontSize: '1.25rem', fontWeight: 800 }}>Phân quyền tài khoản</h3>
              </div>

              <div style={{ marginBottom: '1.5rem', background: '#f8fafc', padding: '1rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.9rem', marginBottom: '0.35rem' }}>
                  <strong>Tài khoản:</strong> {selectedUser?.username}
                </div>
                <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  <strong>Email:</strong> {selectedUser?.email || '-'}
                </div>
                <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                  <strong>Vai trò hiện tại:</strong> <Badge variant={ROLE_BADGE[selectedUser?.role] || 'neutral'}>{selectedUser?.role}</Badge>
                </div>
              </div>

              <div className="form-group" style={{ marginBottom: '1.5rem' }}>
                <label className="form-label" style={{ fontWeight: 700 }}>Chọn vai trò mới:</label>
                <select
                  value={newRole}
                  onChange={(e) => setNewRole(e.target.value)}
                  className="form-select"
                  style={{ fontSize: '0.95rem', padding: '0.6rem' }}
                >
                  <option value="ADMIN">ADMIN (Quản trị viên toàn quyền)</option>
                  <option value="PRODUCT_OWNER">PRODUCT_OWNER (Quản lý sản phẩm, duyệt đơn)</option>
                  <option value="CUSTOMER">CUSTOMER (Khách hàng mua hàng & hóa đơn)</option>
                </select>
              </div>

              <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                <button
                  type="button"
                  onClick={() => setIsRoleModalOpen(false)}
                  className="btn btn-secondary"
                  disabled={actionLoading}
                >
                  Hủy bỏ
                </button>
                <button
                  type="button"
                  onClick={handleSaveRole}
                  disabled={actionLoading}
                  className="btn btn-primary"
                >
                  <CheckCircle2 size={16} /> {actionLoading ? 'Đang cập nhật...' : 'Xác nhận phân quyền'}
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};

export default UserManagementPage;
