import React, { useState, useEffect } from 'react';
import Sidebar from '../../components/admin/Sidebar';
import ProductModal from '../../components/admin/ProductModal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Pagination from '../../components/common/Pagination';
import Badge from '../../components/common/Badge';
import productService from '../../services/product.service';
import categoryService from '../../services/category.service';
import { formatCurrency } from '../../utils/formatters';
import { useToast } from '../../hooks/useToast';
import { useAuth } from '../../hooks/useAuth';
import { Package, Plus, Search, Edit2, Trash2, Filter, SlidersHorizontal, RotateCcw, Tag, Eye, Lock, ShieldAlert } from 'lucide-react';

export const ProductManagementPage = () => {
  const { success, error } = useToast();
  const { user, isAdmin, isProductOwner } = useAuth();
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [sortBy, setSortBy] = useState('id,desc');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const [selectedProduct, setSelectedProduct] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isViewOnlyModal, setIsViewOnlyModal] = useState(false);
  const [modalLoading, setModalLoading] = useState(false);

  const canManageProduct = (product) => {
    if (isAdmin()) return true;
    if (!isProductOwner()) return false;
    if (!product || !product.categoryId) return false;
    if (product.managedByUsername && user?.username) {
      return product.managedByUsername.toLowerCase() === user.username.toLowerCase();
    }
    if (product.managedByUserId && user?.id) {
      return Number(product.managedByUserId) === Number(user.id);
    }
    return false;
  };

  useEffect(() => {
    categoryService.getCategories({ size: 100 })
      .then((res) => setCategories(res?.content || res || []))
      .catch((err) => console.error('Failed to load categories for filter:', err));
  }, []);

  const fetchProducts = async (targetPage = page, overrideFilters = {}) => {
    setLoading(true);
    try {
      const currentKeyword = overrideFilters.keyword !== undefined ? overrideFilters.keyword : keyword;
      const currentStatus = overrideFilters.status !== undefined ? overrideFilters.status : statusFilter;
      const currentCategory = overrideFilters.categoryId !== undefined ? overrideFilters.categoryId : categoryFilter;
      const currentMinPrice = overrideFilters.minPrice !== undefined ? overrideFilters.minPrice : minPrice;
      const currentMaxPrice = overrideFilters.maxPrice !== undefined ? overrideFilters.maxPrice : maxPrice;
      const currentSortBy = overrideFilters.sortBy !== undefined ? overrideFilters.sortBy : sortBy;

      const res = await productService.getProducts({
        page: targetPage,
        size: 10,
        keyword: currentKeyword.trim() || undefined,
        status: currentStatus || undefined,
        categoryId: currentCategory ? Number(currentCategory) : undefined,
        minPrice: currentMinPrice !== '' ? Number(currentMinPrice) : undefined,
        maxPrice: currentMaxPrice !== '' ? Number(currentMaxPrice) : undefined,
        sortBy: currentSortBy,
      });
      if (res && res.content) {
        setProducts(res.content);
        setTotalPages(res.totalPages || 1);
      } else if (Array.isArray(res)) {
        setProducts(res);
        setTotalPages(1);
      }
    } catch (err) {
      error(err.message || 'Không thể tải danh sách sản phẩm.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts(page, { status: statusFilter, categoryId: categoryFilter, sortBy });
  }, [page, statusFilter, categoryFilter, sortBy]);

  const handleSearch = (e) => {
    if (e) e.preventDefault();
    if (page !== 0) {
      setPage(0);
    } else {
      fetchProducts(0);
    }
  };

  const handleResetFilters = () => {
    setKeyword('');
    setStatusFilter('');
    setCategoryFilter('');
    setSortBy('id,desc');
    setMinPrice('');
    setMaxPrice('');
    if (page !== 0) {
      setPage(0);
    } else {
      fetchProducts(0, { keyword: '', status: '', categoryId: '', minPrice: '', maxPrice: '', sortBy: 'id,desc' });
    }
  };

  const handleOpenCreate = () => {
    setSelectedProduct(null);
    setIsViewOnlyModal(false);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (product) => {
    setSelectedProduct(product);
    setIsViewOnlyModal(!canManageProduct(product));
    setIsModalOpen(true);
  };

  const handleOpenView = (product) => {
    setSelectedProduct(product);
    setIsViewOnlyModal(true);
    setIsModalOpen(true);
  };

  const handleSaveProduct = async (productData) => {
    setModalLoading(true);
    try {
      if (selectedProduct) {
        await productService.updateProduct(selectedProduct.id, productData);
        success('Cập nhật thông tin sản phẩm thành công!');
      } else {
        await productService.createProduct(productData);
        success('Thêm sản phẩm mới thành công!');
      }
      setIsModalOpen(false);
      fetchProducts();
    } catch (err) {
      error(err.message || 'Lỗi khi lưu sản phẩm.');
    } finally {
      setModalLoading(false);
    }
  };

  const handleDeleteProduct = async (id) => {
    if (!window.confirm('Bạn có chắc muốn xóa sản phẩm này?')) return;
    try {
      await productService.deleteProduct(id);
      success('Đã xóa sản phẩm thành công!');
      fetchProducts();
    } catch (err) {
      error(err.message || 'Lỗi khi xóa sản phẩm.');
    }
  };

  return (
    <div style={{ display: 'flex' }}>
      <Sidebar />

      <main style={{ flex: 1, padding: '2rem' }} className="animate-fade-in">
        <div className="page-header">
          <div>
            <h1 className="page-title">Quản lý Sản phẩm</h1>
            <p className="page-subtitle">Danh mục sản phẩm, giá bán, tồn kho và bộ lọc tìm kiếm nâng cao</p>
          </div>

          <button onClick={handleOpenCreate} className="btn btn-primary">
            <Plus size={18} /> Thêm sản phẩm mới
          </button>
        </div>

        {/* Search & Filter Controls */}
        <div className="card" style={{ padding: '1.25rem', marginBottom: '1.5rem', background: '#f8fafc', border: '1px solid var(--border-color)' }}>
          <form onSubmit={handleSearch}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', alignItems: 'flex-end' }}>
              {/* Keyword Search */}
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" style={{ fontSize: '0.8rem' }}>Từ khóa tìm kiếm</label>
                <div style={{ position: 'relative' }}>
                  <Search size={18} style={{ position: 'absolute', left: '12px', top: '10px', color: 'var(--text-sub)' }} />
                  <input
                    type="text"
                    placeholder="Tìm theo tên sản phẩm..."
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                    className="form-control"
                    style={{ paddingLeft: '2.5rem' }}
                  />
                </div>
              </div>

              {/* Category Filter */}
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" style={{ fontSize: '0.8rem' }}>Loại hàng hóa</label>
                <select
                  value={categoryFilter}
                  onChange={(e) => {
                    setCategoryFilter(e.target.value);
                    setPage(0);
                  }}
                  className="form-select"
                >
                  <option value="">Tất cả loại hàng</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      [{c.code}] {c.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Status Filter */}
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" style={{ fontSize: '0.8rem' }}>Trạng thái kinh doanh</label>
                <select
                  value={statusFilter}
                  onChange={(e) => {
                    setStatusFilter(e.target.value);
                    setPage(0);
                  }}
                  className="form-select"
                >
                  <option value="">Tất cả trạng thái</option>
                  <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                  <option value="INACTIVE">Ngừng hoạt động (INACTIVE)</option>
                </select>
              </div>

              {/* Sort By Selector */}
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" style={{ fontSize: '0.8rem' }}>Sắp xếp theo</label>
                <select
                  value={sortBy}
                  onChange={(e) => {
                    setSortBy(e.target.value);
                    setPage(0);
                  }}
                  className="form-select"
                >
                  <option value="id,desc">Mới nhất (ID giảm dần)</option>
                  <option value="id,asc">Cũ nhất (ID tăng dần)</option>
                  <option value="price,asc">Giá: Thấp đến Cao</option>
                  <option value="price,desc">Giá: Cao đến Thấp</option>
                  <option value="name,asc">Tên sản phẩm: A - Z</option>
                  <option value="stock,desc">Tồn kho: Nhiều nhất</option>
                  <option value="stock,asc">Tồn kho: Ít nhất</option>
                </select>
              </div>

              {/* Price Range */}
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" style={{ fontSize: '0.8rem' }}>Khoảng giá (VND)</label>
                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                  <input
                    type="number"
                    min="0"
                    placeholder="Min"
                    value={minPrice}
                    onChange={(e) => setMinPrice(e.target.value)}
                    className="form-control"
                    style={{ padding: '0.45rem 0.6rem', fontSize: '0.85rem' }}
                  />
                  <span>-</span>
                  <input
                    type="number"
                    min="0"
                    placeholder="Max"
                    value={maxPrice}
                    onChange={(e) => setMaxPrice(e.target.value)}
                    className="form-control"
                    style={{ padding: '0.45rem 0.6rem', fontSize: '0.85rem' }}
                  />
                </div>
              </div>

              {/* Action Buttons */}
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
          <LoadingSpinner text="Đang tải danh sách sản phẩm..." />
        ) : products.length === 0 ? (
          <div style={{
            textAlign: 'center',
            padding: '4rem 2rem',
            background: 'white',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border-color)',
            color: 'var(--text-muted)',
          }}>
            <Package size={48} style={{ margin: '0 auto 1rem auto', opacity: 0.6 }} />
            <p style={{ fontSize: '1.1rem', fontWeight: 600 }}>Không tìm thấy sản phẩm nào.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Ảnh</th>
                  <th>Tên sản phẩm</th>
                  <th>Loại hàng</th>
                  <th>Đơn giá</th>
                  <th>Tồn kho</th>
                  <th>Trạng thái</th>
                  <th style={{ textAlign: 'right' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {products.map((p) => (
                  <tr key={p.id}>
                    <td>#{p.id}</td>
                    <td>
                      <div style={{
                        width: '44px',
                        height: '44px',
                        borderRadius: 'var(--radius-sm)',
                        backgroundColor: '#f1f5f9',
                        overflow: 'hidden',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        border: '1px solid var(--border-color)',
                      }}>
                        {p.primaryImageUrl || (p.imageUrls && p.imageUrls[0]) ? (
                          <img
                            src={p.primaryImageUrl || p.imageUrls[0]}
                            alt={p.name}
                            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                          />
                        ) : (
                          <span style={{ fontSize: '1.2rem', opacity: 0.6 }}>📦</span>
                        )}
                      </div>
                    </td>
                    <td>
                      <strong>{p.name}</strong>
                      {p.imageUrls && p.imageUrls.length > 1 && (
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          {p.imageUrls.length} ảnh
                        </div>
                      )}
                    </td>
                    <td>
                      {p.categoryName || p.categoryCode ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '3px' }}>
                          <span style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.25rem',
                            fontSize: '0.78rem',
                            fontWeight: 600,
                            backgroundColor: '#f1f5f9',
                            color: '#334155',
                            padding: '3px 8px',
                            borderRadius: '12px',
                            border: '1px solid #cbd5e1',
                            width: 'fit-content',
                          }}>
                            <Tag size={12} color="var(--color-primary)" />
                            {p.categoryName || p.categoryCode}
                          </span>
                          {isProductOwner() && (
                            canManageProduct(p) ? (
                              <span style={{ fontSize: '0.7rem', color: '#16a34a', fontWeight: 600 }}>
                                ✓ Bạn phụ trách
                              </span>
                            ) : (
                              <span style={{ fontSize: '0.7rem', color: '#64748b' }}>
                                PO: {p.managedByUsername || 'Chưa phân công'}
                              </span>
                            )
                          )}
                        </div>
                      ) : (
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>— Chưa gán —</span>
                      )}
                    </td>
                    <td><strong style={{ color: 'var(--color-primary)' }}>{formatCurrency(p.price)}</strong></td>
                    <td>
                      <span style={{ fontWeight: 600, color: p.stock === 0 ? 'var(--color-danger)' : 'var(--text-main)' }}>
                        {p.stock} cái
                      </span>
                    </td>
                    <td>
                      <Badge variant={p.status === 'ACTIVE' ? 'success' : 'danger'}>
                        {p.status === 'ACTIVE' ? 'Hoạt động' : 'Ngừng hoạt động'}
                      </Badge>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '0.5rem', alignItems: 'center' }}>
                        {canManageProduct(p) ? (
                          <>
                            <button
                              onClick={() => handleOpenEdit(p)}
                              className="btn btn-sm btn-outline"
                              title="Sửa sản phẩm"
                            >
                              <Edit2 size={14} /> Sửa
                            </button>
                            <button
                              onClick={() => handleDeleteProduct(p.id)}
                              className="btn btn-sm btn-danger"
                              title="Xóa sản phẩm"
                            >
                              <Trash2 size={14} />
                            </button>
                          </>
                        ) : (
                          <>
                            <button
                              onClick={() => handleOpenView(p)}
                              className="btn btn-sm btn-outline"
                              title="Xem chi tiết sản phẩm"
                            >
                              <Eye size={14} /> Xem
                            </button>
                            <span
                              style={{
                                fontSize: '0.7rem',
                                color: '#94a3b8',
                                backgroundColor: '#f1f5f9',
                                padding: '2px 6px',
                                borderRadius: '4px',
                                border: '1px solid #e2e8f0',
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '3px'
                              }}
                              title="Bạn không quản lý loại hàng này nên chỉ có quyền xem"
                            >
                              <Lock size={10} /> Chỉ xem
                            </span>
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

        {/* Create/Edit/View Modal */}
        <ProductModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSave={handleSaveProduct}
          product={selectedProduct}
          loading={modalLoading}
          readOnly={isViewOnlyModal}
        />
      </main>
    </div>
  );
};

export default ProductManagementPage;
