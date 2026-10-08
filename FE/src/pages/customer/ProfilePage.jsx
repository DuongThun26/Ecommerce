import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import userService from '../../services/user.service';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { formatDateTime } from '../../utils/formatters';
import {
  User,
  Mail,
  Phone,
  Shield,
  Key,
  Layers,
  Save,
  CheckCircle,
  AlertCircle,
  Eye,
  EyeOff,
  ShoppingBag,
  FileText,
  Clock,
  UserCheck,
  Check,
  ArrowRight
} from 'lucide-react';

export const ProfilePage = () => {
  const { user, isAdmin, isProductOwner } = useAuth();
  const { success, error } = useToast();

  const [activeTab, setActiveTab] = useState('info'); // 'info' | 'password' | 'role'
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  // Form states for profile info
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: ''
  });
  const [savingProfile, setSavingProfile] = useState(false);

  // Form states for password change
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    setLoading(true);
    try {
      const res = await userService.getMyProfile();
      if (res) {
        setProfile(res);
        setFormData({
          name: res.name || '',
          email: res.email || '',
          phone: res.phone || ''
        });
      }
    } catch (err) {
      console.error('Failed to load profile:', err);
      error(err.message || 'Không thể tải thông tin cá nhân');
    } finally {
      setLoading(false);
    }
  };

  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      error('Họ và tên không được để trống');
      return;
    }
    if (!formData.email.trim()) {
      error('Email không được để trống');
      return;
    }

    setSavingProfile(true);
    try {
      const updated = await userService.updateMyProfile({
        name: formData.name.trim(),
        email: formData.email.trim(),
        phone: formData.phone.trim()
      });
      setProfile(updated);
      success('Cập nhật thông tin cá nhân thành công!');
    } catch (err) {
      console.error('Failed to update profile:', err);
      error(err.message || 'Cập nhật thông tin thất bại');
    } finally {
      setSavingProfile(false);
    }
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    if (!passwordData.currentPassword) {
      error('Vui lòng nhập mật khẩu hiện tại');
      return;
    }
    if (!passwordData.newPassword) {
      error('Vui lòng nhập mật khẩu mới');
      return;
    }
    if (passwordData.newPassword.length < 6) {
      error('Mật khẩu mới phải có ít nhất 6 ký tự');
      return;
    }
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      error('Mật khẩu xác nhận không khớp');
      return;
    }
    if (passwordData.currentPassword === passwordData.newPassword) {
      error('Mật khẩu mới không được trùng với mật khẩu hiện tại');
      return;
    }

    setSavingPassword(true);
    try {
      await userService.changePassword({
        currentPassword: passwordData.currentPassword,
        newPassword: passwordData.newPassword,
        confirmPassword: passwordData.confirmPassword
      });
      success('Đổi mật khẩu thành công!');
      setPasswordData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      });
    } catch (err) {
      console.error('Failed to change password:', err);
      error(err.message || 'Đổi mật khẩu thất bại');
    } finally {
      setSavingPassword(false);
    }
  };

  const getRoleBadgeVariant = (role) => {
    switch (role) {
      case 'ADMIN':
        return 'primary';
      case 'PRODUCT_OWNER':
        return 'info';
      default:
        return 'success';
    }
  };

  const getRoleDisplayName = (role) => {
    switch (role) {
      case 'ADMIN':
        return 'Quản trị viên (Admin)';
      case 'PRODUCT_OWNER':
        return 'Product Owner (PO)';
      case 'CUSTOMER':
        return 'Khách hàng (Customer)';
      default:
        return role || 'Người dùng';
    }
  };

  if (loading) {
    return (
      <div style={{ maxWidth: '1000px', margin: '2rem auto', padding: '0 1.5rem', textAlign: 'center' }}>
        <LoadingSpinner />
        <p style={{ marginTop: '1rem', color: 'var(--text-muted)' }}>Đang tải thông tin tài khoản...</p>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '1000px', margin: '2rem auto', padding: '0 1.5rem' }}>
      {/* Header Profile Card */}
      <div style={{
        background: 'white',
        borderRadius: 'var(--radius-lg)',
        boxShadow: 'var(--shadow-sm)',
        border: '1px solid var(--border-color)',
        padding: '2rem',
        marginBottom: '2rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '1.5rem'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          <div style={{
            width: '72px',
            height: '72px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #4f46e5, #7c3aed)',
            color: 'white',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '1.75rem',
            fontWeight: 700,
            boxShadow: '0 4px 12px rgba(79, 70, 229, 0.25)',
            textTransform: 'uppercase'
          }}>
            {profile?.name ? profile.name.charAt(0) : (profile?.username ? profile.username.charAt(0) : 'U')}
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
              <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-main)', margin: 0 }}>
                {profile?.name || profile?.username}
              </h1>
              <Badge variant={getRoleBadgeVariant(profile?.role)}>
                {getRoleDisplayName(profile?.role)}
              </Badge>
              <Badge variant={profile?.status === 'ACTIVE' ? 'success' : 'danger'}>
                {profile?.status === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm khóa'}
              </Badge>
            </div>
            <p style={{ color: 'var(--text-muted)', margin: '0.35rem 0 0 0', fontSize: '0.9rem' }}>
              @{profile?.username} • Tham gia từ {formatDateTime(profile?.createdAt)}
            </p>
          </div>
        </div>

        {/* Quick info badges */}
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
          {profile?.customerId && (
            <div style={{
              background: 'var(--bg-secondary)',
              padding: '0.5rem 1rem',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-color)',
              textAlign: 'center'
            }}>
              <span style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600 }}>MÃ KHÁCH HÀNG</span>
              <span style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--color-primary)' }}>#{profile.customerId}</span>
            </div>
          )}
        </div>
      </div>

      {/* Tabs Navigation */}
      <div style={{
        display: 'flex',
        gap: '0.5rem',
        borderBottom: '2px solid var(--border-color)',
        marginBottom: '2rem'
      }}>
        <button
          onClick={() => setActiveTab('info')}
          style={{
            padding: '0.75rem 1.25rem',
            fontWeight: 700,
            fontSize: '0.95rem',
            border: 'none',
            background: 'none',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            color: activeTab === 'info' ? 'var(--color-primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'info' ? '3px solid var(--color-primary)' : '3px solid transparent',
            marginBottom: '-2px',
            transition: 'all var(--transition-fast)'
          }}
        >
          <User size={18} /> Thông tin cá nhân
        </button>

        <button
          onClick={() => setActiveTab('password')}
          style={{
            padding: '0.75rem 1.25rem',
            fontWeight: 700,
            fontSize: '0.95rem',
            border: 'none',
            background: 'none',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            color: activeTab === 'password' ? 'var(--color-primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'password' ? '3px solid var(--color-primary)' : '3px solid transparent',
            marginBottom: '-2px',
            transition: 'all var(--transition-fast)'
          }}
        >
          <Key size={18} /> Đổi mật khẩu
        </button>

        <button
          onClick={() => setActiveTab('role')}
          style={{
            padding: '0.75rem 1.25rem',
            fontWeight: 700,
            fontSize: '0.95rem',
            border: 'none',
            background: 'none',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            color: activeTab === 'role' ? 'var(--color-primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'role' ? '3px solid var(--color-primary)' : '3px solid transparent',
            marginBottom: '-2px',
            transition: 'all var(--transition-fast)'
          }}
        >
          <Shield size={18} /> Vai trò & Quyền hạn
        </button>
      </div>

      {/* Tab 1: Profile Info Form */}
      {activeTab === 'info' && (
        <div style={{
          background: 'white',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-sm)',
          border: '1px solid var(--border-color)',
          padding: '2rem'
        }}>
          <h2 style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <UserCheck size={20} color="var(--color-primary)" /> Chỉnh sửa thông tin tài khoản
          </h2>

          <form onSubmit={handleProfileSubmit}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.5rem', marginBottom: '1.5rem' }}>
              {/* Username (Disabled) */}
              <div className="form-group">
                <label className="form-label" style={{ fontWeight: 600 }}>Tên đăng nhập (Username)</label>
                <input
                  type="text"
                  className="form-control"
                  value={profile?.username || ''}
                  disabled
                  style={{ background: '#f1f5f9', cursor: 'not-allowed', color: '#64748b' }}
                />
                <small style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginTop: '0.25rem', display: 'block' }}>
                  Tên đăng nhập là định danh duy nhất và không thể thay đổi.
                </small>
              </div>

              {/* Full Name */}
              <div className="form-group">
                <label className="form-label" style={{ fontWeight: 600 }}>Họ và tên <span style={{ color: 'var(--color-danger)' }}>*</span></label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Nhập họ và tên đầy đủ"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                />
              </div>

              {/* Email */}
              <div className="form-group">
                <label className="form-label" style={{ fontWeight: 600 }}>Email liên hệ <span style={{ color: 'var(--color-danger)' }}>*</span></label>
                <div style={{ position: 'relative' }}>
                  <input
                    type="email"
                    className="form-control"
                    placeholder="user@example.com"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    required
                  />
                  <Mail size={18} style={{ position: 'absolute', right: '12px', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
                </div>
              </div>

              {/* Phone */}
              <div className="form-group">
                <label className="form-label" style={{ fontWeight: 600 }}>Số điện thoại</label>
                <div style={{ position: 'relative' }}>
                  <input
                    type="tel"
                    className="form-control"
                    placeholder="0987654321"
                    value={formData.phone}
                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                  />
                  <Phone size={18} style={{ position: 'absolute', right: '12px', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', paddingTop: '1rem', borderTop: '1px solid var(--border-color)' }}>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={savingProfile}
                style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', minWidth: '160px', justifyContent: 'center' }}
              >
                {savingProfile ? (
                  <>
                    <LoadingSpinner size="sm" color="white" /> Đang lưu...
                  </>
                ) : (
                  <>
                    <Save size={18} /> Lưu thay đổi
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Tab 2: Change Password Form */}
      {activeTab === 'password' && (
        <div style={{
          background: 'white',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-sm)',
          border: '1px solid var(--border-color)',
          padding: '2rem',
          maxWidth: '600px'
        }}>
          <h2 style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Key size={20} color="var(--color-primary)" /> Đổi mật khẩu bảo vệ tài khoản
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginBottom: '1.5rem' }}>
            Để đảm bảo an toàn, vui lòng chọn mật khẩu mạnh có tối thiểu 6 ký tự.
          </p>

          <form onSubmit={handlePasswordSubmit}>
            {/* Current Password */}
            <div className="form-group" style={{ marginBottom: '1.25rem' }}>
              <label className="form-label" style={{ fontWeight: 600 }}>Mật khẩu hiện tại <span style={{ color: 'var(--color-danger)' }}>*</span></label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showCurrentPassword ? 'text' : 'password'}
                  className="form-control"
                  placeholder="Nhập mật khẩu hiện tại"
                  value={passwordData.currentPassword}
                  onChange={(e) => setPasswordData({ ...passwordData, currentPassword: e.target.value })}
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                  style={{
                    position: 'absolute',
                    right: '10px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    color: '#94a3b8'
                  }}
                >
                  {showCurrentPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            {/* New Password */}
            <div className="form-group" style={{ marginBottom: '1.25rem' }}>
              <label className="form-label" style={{ fontWeight: 600 }}>Mật khẩu mới <span style={{ color: 'var(--color-danger)' }}>*</span></label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showNewPassword ? 'text' : 'password'}
                  className="form-control"
                  placeholder="Tối thiểu 6 ký tự"
                  value={passwordData.newPassword}
                  onChange={(e) => setPasswordData({ ...passwordData, newPassword: e.target.value })}
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowNewPassword(!showNewPassword)}
                  style={{
                    position: 'absolute',
                    right: '10px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    color: '#94a3b8'
                  }}
                >
                  {showNewPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            {/* Confirm New Password */}
            <div className="form-group" style={{ marginBottom: '1.5rem' }}>
              <label className="form-label" style={{ fontWeight: 600 }}>Xác nhận mật khẩu mới <span style={{ color: 'var(--color-danger)' }}>*</span></label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  className="form-control"
                  placeholder="Nhập lại mật khẩu mới"
                  value={passwordData.confirmPassword}
                  onChange={(e) => setPasswordData({ ...passwordData, confirmPassword: e.target.value })}
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  style={{
                    position: 'absolute',
                    right: '10px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    color: '#94a3b8'
                  }}
                >
                  {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', paddingTop: '1rem', borderTop: '1px solid var(--border-color)' }}>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={savingPassword}
                style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', minWidth: '180px', justifyContent: 'center' }}
              >
                {savingPassword ? (
                  <>
                    <LoadingSpinner size="sm" color="white" /> Đang cập nhật...
                  </>
                ) : (
                  <>
                    <Save size={18} /> Cập nhật mật khẩu
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Tab 3: Role & Responsibilities */}
      {activeTab === 'role' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* PO View */}
          {profile?.role === 'PRODUCT_OWNER' && (
            <div style={{
              background: 'white',
              borderRadius: 'var(--radius-lg)',
              boxShadow: 'var(--shadow-sm)',
              border: '1px solid var(--border-color)',
              padding: '2rem'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
                <div>
                  <h2 style={{ fontSize: '1.2rem', fontWeight: 700, margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Layers size={20} color="var(--color-primary)" /> Danh mục loại hàng phụ trách
                  </h2>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
                    Bạn có thẩm quyền kiểm duyệt các phiếu mua hàng và quản lý sản phẩm thuộc các ngành hàng này.
                  </p>
                </div>
                <Link to="/admin/tickets" className="btn btn-sm btn-primary">
                  Đến trang duyệt phiếu PO <ArrowRight size={16} />
                </Link>
              </div>

              {profile?.managedCategories && profile.managedCategories.length > 0 ? (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: '1rem' }}>
                  {profile.managedCategories.map((cat) => (
                    <div
                      key={cat.id}
                      style={{
                        border: '1px solid var(--border-color)',
                        borderRadius: 'var(--radius-md)',
                        padding: '1.25rem',
                        background: 'var(--bg-secondary)',
                        transition: 'transform var(--transition-fast)'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                        <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--color-primary)', background: 'var(--color-primary-light)', padding: '0.2rem 0.5rem', borderRadius: '4px' }}>
                          MÃ: {cat.code}
                        </span>
                        <Badge variant="info">Phụ trách</Badge>
                      </div>
                      <h4 style={{ margin: '0.5rem 0', fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-main)' }}>
                        {cat.name}
                      </h4>
                    </div>
                  ))}
                </div>
              ) : (
                <div style={{ textAlign: 'center', padding: '2.5rem 1rem', background: '#f8fafc', borderRadius: 'var(--radius-md)' }}>
                  <AlertCircle size={40} color="var(--color-warning)" style={{ marginBottom: '0.75rem' }} />
                  <p style={{ fontWeight: 600, color: 'var(--text-main)', margin: 0 }}>Chưa được phân công ngành hàng nào</p>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
                    Vui lòng liên hệ Quản trị viên (Admin) để được cấp quyền quản lý loại hàng tương ứng.
                  </p>
                </div>
              )}
            </div>
          )}

          {/* Admin View */}
          {profile?.role === 'ADMIN' && (
            <div style={{
              background: 'white',
              borderRadius: 'var(--radius-lg)',
              boxShadow: 'var(--shadow-sm)',
              border: '1px solid var(--border-color)',
              padding: '2rem'
            }}>
              <h2 style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Shield size={20} color="var(--color-primary)" /> Quyền hạn Quản trị viên Toàn quyền (Super Admin)
              </h2>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginBottom: '1.5rem' }}>
                Tài khoản của bạn có toàn quyền quản lý toàn bộ hệ thống E-Commerce Enterprise.
              </p>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
                <div style={{ padding: '1rem', borderRadius: 'var(--radius-md)', background: '#f8fafc', border: '1px solid var(--border-color)' }}>
                  <h4 style={{ margin: '0 0 0.5rem 0', fontWeight: 700 }}>Tổng quan hệ thống</h4>
                  <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)', margin: '0 0 0.75rem 0' }}>Theo dõi doanh thu, báo cáo tổng thể.</p>
                  <Link to="/admin/dashboard" className="btn btn-xs btn-outline">Xem Dashboard</Link>
                </div>
                <div style={{ padding: '1rem', borderRadius: 'var(--radius-md)', background: '#f8fafc', border: '1px solid var(--border-color)' }}>
                  <h4 style={{ margin: '0 0 0.5rem 0', fontWeight: 700 }}>Quản lý người dùng</h4>
                  <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)', margin: '0 0 0.75rem 0' }}>Phân quyền PO, kích hoạt/khóa tài khoản.</p>
                  <Link to="/admin/users" className="btn btn-xs btn-outline">Quản lý Users</Link>
                </div>
                <div style={{ padding: '1rem', borderRadius: 'var(--radius-md)', background: '#f8fafc', border: '1px solid var(--border-color)' }}>
                  <h4 style={{ margin: '0 0 0.5rem 0', fontWeight: 700 }}>Phân công loại hàng</h4>
                  <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)', margin: '0 0 0.75rem 0' }}>Gán Product Owner cho từng loại sản phẩm.</p>
                  <Link to="/admin/categories" className="btn btn-xs btn-outline">Phân công PO</Link>
                </div>
              </div>
            </div>
          )}

          {/* Customer View */}
          {profile?.role === 'CUSTOMER' && (
            <div style={{
              background: 'white',
              borderRadius: 'var(--radius-lg)',
              boxShadow: 'var(--shadow-sm)',
              border: '1px solid var(--border-color)',
              padding: '2rem'
            }}>
              <h2 style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <ShoppingBag size={20} color="var(--color-primary)" /> Tài khoản Khách hàng mua sắm
              </h2>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginBottom: '1.5rem' }}>
                Truy cập nhanh các đơn đặt hàng, hóa đơn điện tử VAT và lịch sử mua sắm của bạn.
              </p>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
                <Link
                  to="/orders"
                  style={{
                    padding: '1.25rem',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--border-color)',
                    background: '#f8fafc',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '1rem',
                    color: 'var(--text-main)',
                    textDecoration: 'none'
                  }}
                >
                  <div style={{ background: 'var(--color-primary-light)', color: 'var(--color-primary)', width: '40px', height: '40px', borderRadius: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <ShoppingBag size={20} />
                  </div>
                  <div>
                    <h4 style={{ margin: 0, fontSize: '0.95rem', fontWeight: 700 }}>Đơn hàng của tôi</h4>
                    <p style={{ margin: 0, fontSize: '0.8rem', color: 'var(--text-muted)' }}>Theo dõi trạng thái & chi tiết</p>
                  </div>
                </Link>

                <Link
                  to="/invoices"
                  style={{
                    padding: '1.25rem',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--border-color)',
                    background: '#f8fafc',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '1rem',
                    color: 'var(--text-main)',
                    textDecoration: 'none'
                  }}
                >
                  <div style={{ background: '#e0e7ff', color: '#4338ca', width: '40px', height: '40px', borderRadius: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <FileText size={20} />
                  </div>
                  <div>
                    <h4 style={{ margin: 0, fontSize: '0.95rem', fontWeight: 700 }}>Hóa đơn VAT</h4>
                    <p style={{ margin: 0, fontSize: '0.8rem', color: 'var(--text-muted)' }}>Xuất hóa đơn & thanh toán</p>
                  </div>
                </Link>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default ProfilePage;
