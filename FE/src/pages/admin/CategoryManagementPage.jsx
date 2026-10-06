import React, { useState, useEffect } from 'react';
import Sidebar from '../../components/admin/Sidebar';
import CategoryModal from '../../components/admin/CategoryModal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Pagination from '../../components/common/Pagination';
import Badge from '../../components/common/Badge';
import categoryService from '../../services/category.service';
import userService from '../../services/user.service';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { Layers, Plus, Search, Edit2, Trash2, UserCheck, Shield, Package, RotateCcw, Eye, Lock } from 'lucide-react';

export const CategoryManagementPage = () => {
  const { success, error } = useToast();
  const { user, isAdmin, isProductOwner } = useAuth();
  const [categories, setCategories] = useState([]);
  const [poUsers, setPoUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [poFilter, setPoFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const [selectedCategory, setSelectedCategory] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isViewOnlyModal, setIsViewOnlyModal] = useState(false);
  const [modalLoading, setModalLoading] = useState(false);

  const fetchCategories = async (targetPage = page, overrideFilters = {}) => {
    setLoading(true);
    try {
      const currentKeyword = overrideFilters.keyword !== undefined ? overrideFilters.keyword : keyword;
      const currentPo = overrideFilters.managedByUserId !== undefined ? overrideFilters.managedByUserId : poFilter;
      const managedByUsername = (!isAdmin() && isProductOwner() && user?.username) ? user.username : undefined;

      const res = await categoryService.getCategories({
        page: targetPage,
        size: 10,
        keyword: currentKeyword.trim() || undefined,
        managedByUserId: isAdmin() ? (currentPo || undefined) : undefined,
        managedByUsername: managedByUsername,
      });

      if (res && res.content) {
        setCategories(res.content);
        setTotalPages(res.totalPages || 1);
      } else if (Array.isArray(res)) {
        setCategories(res);
        setTotalPages(1);
      }
    } catch (err) {
      error(err.message || 'Không thể tải danh sách loại hàng.');
    } finally {
      setLoading(false);
    }
  };

  const fetchPoUsers = async () => {
    try {
      const res = await userService.getUsers();
      const usersList = Array.isArray(res) ? res : (res?.content || []);
      setPoUsers(usersList.filter(u => u.role === 'PRODUCT_OWNER' || u.role === 'ADMIN'));
    } catch (err) {
      console.error('Không thể lấy danh sách PO:', err);
    }
  };

  useEffect(() => {
    if (isAdmin()) {
      fetchPoUsers();
    }
  }, []);

  useEffect(() => {
    fetchCategories(page, { managedByUserId: poFilter });
  }, [page, poFilter, user?.username]);

  const handleSearch = (e) => {
    if (e) e.preventDefault();
    setPage(0);
    fetchCategories(0);
  };

  const handleResetFilters = () => {
    setKeyword('');
    setPoFilter('');
    setPage(0);
    fetchCategories(0, { keyword: '', managedByUserId: '' });
  };

  const handleOpenCreate = () => {
    setSelectedCategory(null);
    setIsViewOnlyModal(false);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (category) => {
    setSelectedCategory(category);
    setIsViewOnlyModal(false);
    setIsModalOpen(true);
  };

  const handleOpenView = (category) => {
    setSelectedCategory(category);
    setIsViewOnlyModal(true);
    setIsModalOpen(true);
  };

  const handleSaveCategory = async (categoryData) => {
    setModalLoading(true);
    try {
      if (selectedCategory) {
        await categoryService.updateCategory(selectedCategory.id, categoryData);
        success(`Cập nhật loại hàng "${categoryData.name}" thành công!`);
      } else {
        await categoryService.createCategory(categoryData);
        success(`Tạo loại hàng "${categoryData.name}" thành công!`);
      }
      setIsModalOpen(false);
      fetchCategories();
    } catch (err) {
      error(err.message || 'Lỗi khi lưu loại hàng.');
    } finally {
      setModalLoading(false);
    }
  };

  const handleDeleteCategory = async (id, name) => {
    if (!window.confirm(`Bạn có chắc muốn xóa loại hàng "${name}"?`)) return;
    try {
      await categoryService.deleteCategory(id);
      success(`Đã xóa loại hàng "${name}" thành công!`);
      fetchCategories();
    } catch (err) {
      error(err.message || 'Lỗi khi xóa loại hàng.');
    }
  };

  return (
    <div style={{ display: 'flex' }}>
      <Sidebar />

      <main style={{ flex: 1, padding: '2rem' }} className="animate-fade-in">
        <div className="page-header">
          <div>
            <h1 className="page-title">
              {isAdmin() ? 'Quản lý Loại hàng & Phân công PO' : 'Loại hàng được phân công'}
            </h1>
            <p className="page-subtitle">
              {isAdmin()
                ? 'Cấu hình các ngành hàng / loại sản phẩm và chỉ định Product Owner sở hữu và duyệt từng loại hàng'
                : `Danh sách các loại hàng hóa / ngành hàng được phân công cho tài khoản @${user?.username || 'PO'}`}
            </p>
          </div>

          {isAdmin() && (
            <button onClick={handleOpenCreate} className="btn btn-primary">
              <Plus size={18} /> Thêm loại hàng mới
            </button>
          )}
        </div>

        {/* Filter Card */}
        <div className="card" style={{ padding: '1.25rem', marginBottom: '1.5rem', background: '#f8fafc', border: '1px solid var(--border-color)' }}>
          <form onSubmit={handleSearch}>
            <div style={{ display: 'grid', gridTemplateColumns: isAdmin() ? 'repeat(auto-fit, minmax(240px, 1fr))' : '1fr auto', gap: '1rem', alignItems: 'flex-end' }}>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" style={{ fontSize: '0.8rem' }}>Tìm kiếm</label>
                <div style={{ position: 'relative' }}>
                  <Search size={18} style={{ position: 'absolute', left: '12px', top: '10px', color: 'var(--text-sub)' }} />
                  <input
                    type="text"
                    placeholder="Tìm theo tên hoặc mã loại hàng..."
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                    className="form-control"
                    style={{ paddingLeft: '2.5rem' }}
                  />
                </div>
              </div>

              {isAdmin() && (
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" style={{ fontSize: '0.8rem' }}>Lọc theo PO phụ trách</label>
                  <select
                    value={poFilter}
                    onChange={(e) => {
                      setPoFilter(e.target.value);
                      setPage(0);
                    }}
                    className="form-select"
                  >
                    <option value="">Tất cả Product Owner</option>
                    {poUsers.map(u => (
                      <option key={u.id} value={u.id}>
                        {u.username} ({u.role === 'ADMIN' ? 'Admin' : 'PO'})
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>
                  <Search size={16} /> Lọc
                </button>
                <button
                  type="button"
                  onClick={handleResetFilters}
                  className="btn btn-secondary"
                  title="Đặt lại bộ lọc"
                >
                  <RotateCcw size={16} />
                </button>
              </div>
            </div>
          </form>
        </div>

        {/* Table */}
        {loading ? (
          <LoadingSpinner text="Đang tải danh sách loại hàng..." />
        ) : categories.length === 0 ? (
          <div style={{
            textAlign: 'center',
            padding: '4rem 2rem',
            background: 'white',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border-color)',
            color: 'var(--text-muted)',
          }}>
            <Layers size={48} style={{ margin: '0 auto 1rem auto', opacity: 0.6 }} />
            <p style={{ fontSize: '1.1rem', fontWeight: 600 }}>
              {isAdmin()
                ? 'Không tìm thấy loại hàng nào phù hợp.'
                : 'Bạn chưa được phân công quản lý loại hàng nào.'}
            </p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Mã Code</th>
                  <th>Tên loại hàng</th>
                  <th>Product Owner phụ trách</th>
                  <th>Số sản phẩm</th>
                  <th>Mô tả</th>
                  <th style={{ textAlign: 'right' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {categories.map((c) => (
                  <tr key={c.id}>
                    <td>#{c.id}</td>
                    <td>
                      <span style={{
                        fontFamily: 'monospace',
                        fontWeight: 700,
                        backgroundColor: '#eff6ff',
                        color: 'var(--color-primary)',
                        padding: '3px 8px',
                        borderRadius: '4px',
                        border: '1px solid #bfdbfe',
                        fontSize: '0.85rem'
                      }}>
                        {c.code}
                      </span>
                    </td>
                    <td>
                      <strong style={{ fontSize: '0.95rem' }}>{c.name}</strong>
                    </td>
                    <td>
                      {c.managedBy ? (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          <div style={{
                            width: '28px',
                            height: '28px',
                            borderRadius: '50%',
                            backgroundColor: c.managedBy.username === user?.username ? '#dcfce7' : '#e0e7ff',
                            color: c.managedBy.username === user?.username ? '#15803d' : '#4338ca',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontWeight: 700,
                            fontSize: '0.75rem',
                          }}>
                            {c.managedBy.username?.charAt(0).toUpperCase()}
                          </div>
                          <div>
                            <div style={{ fontWeight: 600, fontSize: '0.85rem' }}>
                              {c.managedBy.username} {c.managedBy.username === user?.username && '(Bạn)'}
                            </div>
                            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                              {c.managedBy.role === 'ADMIN' ? 'Administrator' : 'Product Owner'}
                            </div>
                          </div>
                        </div>
                      ) : (
                        <Badge variant="warning">Chưa phân công</Badge>
                      )}
                    </td>
                    <td>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', fontWeight: 600 }}>
                        <Package size={14} color="var(--text-muted)" />
                        {c.productCount !== undefined ? c.productCount : 0}
                      </span>
                    </td>
                    <td style={{ maxWidth: '240px', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                      {c.description ? (
                        <span title={c.description}>
                          {c.description.length > 50 ? `${c.description.substring(0, 50)}...` : c.description}
                        </span>
                      ) : (
                        <em>Không có</em>
                      )}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '0.5rem', alignItems: 'center' }}>
                        {isAdmin() ? (
                          <>
                            <button
                              onClick={() => handleOpenEdit(c)}
                              className="btn btn-sm btn-outline"
                              title="Sửa loại hàng"
                            >
                              <Edit2 size={14} /> Sửa
                            </button>
                            <button
                              onClick={() => handleDeleteCategory(c.id, c.name)}
                              className="btn btn-sm btn-danger"
                              title="Xóa loại hàng"
                            >
                              <Trash2 size={14} />
                            </button>
                          </>
                        ) : (
                          <>
                            <button
                              onClick={() => handleOpenView(c)}
                              className="btn btn-sm btn-outline"
                              title="Xem chi tiết loại hàng"
                            >
                              <Eye size={14} /> Chi tiết
                            </button>
                            {c.managedBy?.username === user?.username ? (
                              <Badge variant="success">Phụ trách</Badge>
                            ) : (
                              <span style={{ fontSize: '0.72rem', color: '#94a3b8', fontStyle: 'italic' }}>Chỉ xem</span>
                            )}
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <Pagination
              currentPage={page}
              totalPages={totalPages}
              onPageChange={(newPage) => setPage(newPage)}
            />
          </div>
        )}

        {/* Modal */}
        <CategoryModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSave={handleSaveCategory}
          category={selectedCategory}
          loading={modalLoading}
          readOnly={isViewOnlyModal}
        />
      </main>
    </div>
  );
};

export default CategoryManagementPage;
