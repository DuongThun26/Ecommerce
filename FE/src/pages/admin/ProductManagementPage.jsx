import React, { useState, useEffect } from 'react';
import Sidebar from '../../components/admin/Sidebar';
import ProductModal from '../../components/admin/ProductModal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Pagination from '../../components/common/Pagination';
import Badge from '../../components/common/Badge';
import productService from '../../services/product.service';
import { formatCurrency } from '../../utils/formatters';
import { useToast } from '../../hooks/useToast';
import { Package, Plus, Search, Edit2, Trash2 } from 'lucide-react';

export const ProductManagementPage = () => {
  const { success, error } = useToast();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const [selectedProduct, setSelectedProduct] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalLoading, setModalLoading] = useState(false);

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const res = await productService.getProducts({
        page,
        size: 10,
        keyword: keyword.trim() || undefined,
        sortBy: 'id,desc',
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
    fetchProducts();
  }, [page]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    fetchProducts();
  };

  const handleOpenCreate = () => {
    setSelectedProduct(null);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (product) => {
    setSelectedProduct(product);
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
            <p className="page-subtitle">Danh mục sản phẩm, giá bán và quản lý số lượng tồn kho</p>
          </div>

          <button onClick={handleOpenCreate} className="btn btn-primary">
            <Plus size={18} /> Thêm sản phẩm mới
          </button>
        </div>

        {/* Search */}
        <div style={{ marginBottom: '1.5rem', maxWidth: '440px' }}>
          <form onSubmit={handleSearch} style={{ display: 'flex', gap: '0.5rem' }}>
            <div style={{ position: 'relative', flex: 1 }}>
              <Search size={18} style={{ position: 'absolute', left: '12px', top: '12px', color: 'var(--text-sub)' }} />
              <input
                type="text"
                placeholder="Tìm kiếm theo tên sản phẩm..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                className="form-control"
                style={{ paddingLeft: '2.5rem' }}
              />
            </div>
            <button type="submit" className="btn btn-primary">Tìm</button>
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
                  <th>Tên sản phẩm</th>
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
                    <td><strong>{p.name}</strong></td>
                    <td><strong style={{ color: 'var(--color-primary)' }}>{formatCurrency(p.price)}</strong></td>
                    <td>
                      <span style={{ fontWeight: 600, color: p.stock === 0 ? 'var(--color-danger)' : 'var(--text-main)' }}>
                        {p.stock} cái
                      </span>
                    </td>
                    <td>
                      <Badge variant={p.status === 'ACTIVE' ? 'success' : 'neutral'}>
                        {p.status || 'ACTIVE'}
                      </Badge>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '0.5rem' }}>
                        <button
                          onClick={() => handleOpenEdit(p)}
                          className="btn btn-sm btn-outline"
                          title="Sửa"
                        >
                          <Edit2 size={14} /> Sửa
                        </button>
                        <button
                          onClick={() => handleDeleteProduct(p.id)}
                          className="btn btn-sm btn-danger"
                          title="Xóa"
                        >
                          <Trash2 size={14} />
                        </button>
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

        {/* Create/Edit Modal */}
        <ProductModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSave={handleSaveProduct}
          product={selectedProduct}
          loading={modalLoading}
        />
      </main>
    </div>
  );
};

export default ProductManagementPage;
