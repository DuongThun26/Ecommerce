import React, { useState, useEffect } from 'react';
import productService from '../../services/product.service';
import ProductCard from '../../components/customer/ProductCard';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Pagination from '../../components/common/Pagination';
import { Search, SlidersHorizontal, Sparkles, RotateCcw } from 'lucide-react';

export const HomePage = () => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [sortBy, setSortBy] = useState('id,desc');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const res = await productService.getProducts({
        page,
        size: 8,
        keyword: keyword.trim() || undefined,
        minPrice: minPrice !== '' ? Number(minPrice) : undefined,
        maxPrice: maxPrice !== '' ? Number(maxPrice) : undefined,
        sortBy,
      });
      if (res && res.content) {
        setProducts(res.content);
        setTotalPages(res.totalPages || 1);
      } else if (Array.isArray(res)) {
        setProducts(res);
        setTotalPages(1);
      }
    } catch (err) {
      console.error('Error fetching products:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, [page, sortBy]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    fetchProducts();
  };

  const handleResetFilters = () => {
    setKeyword('');
    setMinPrice('');
    setMaxPrice('');
    setSortBy('id,desc');
    setPage(0);
  };

  return (
    <div className="main-content animate-fade-in">
      {/* Hero Banner */}
      <div style={{
        background: 'linear-gradient(135deg, #1e1b4b 0%, #312e81 50%, #4338ca 100%)',
        borderRadius: 'var(--radius-xl)',
        padding: '3rem 2.5rem',
        color: 'white',
        marginBottom: '2.5rem',
        boxShadow: 'var(--shadow-xl)',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <div style={{ maxWidth: '640px', position: 'relative', zIndex: 1 }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem',
            background: 'rgba(255, 255, 255, 0.15)',
            backdropFilter: 'blur(8px)',
            padding: '0.35rem 0.85rem',
            borderRadius: 'var(--radius-full)',
            fontSize: '0.8rem',
            fontWeight: 700,
            marginBottom: '1rem',
            letterSpacing: '0.04em'
          }}>
            <Sparkles size={14} color="#fde047" /> MUA SẮM TRỰC TUYẾN CHÍNH HÃNG
          </div>
          <h1 style={{ fontSize: '2.25rem', fontWeight: 800, lineHeight: 1.2, marginBottom: '0.75rem', letterSpacing: '-0.02em' }}>
            Hệ Thống Mua Hàng & Hóa Đơn VAT Điện Tử
          </h1>
          <p style={{ fontSize: '1rem', opacity: 0.9, lineHeight: 1.6, color: '#e0e7ff' }}>
            Đặt hàng nhanh chóng, quy trình phê duyệt phiếu mua hàng PO minh bạch và tự động phát hành hóa đơn PDF chuẩn hóa.
          </p>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="card" style={{ padding: '1.25rem', marginBottom: '2rem', background: '#f8fafc', border: '1px solid var(--border-color)' }}>
        <form onSubmit={handleSearch}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', alignItems: 'flex-end' }}>
            {/* Keyword Search */}
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label" style={{ fontSize: '0.8rem' }}>Tìm kiếm sản phẩm</label>
              <div style={{ position: 'relative' }}>
                <Search size={18} style={{ position: 'absolute', left: '12px', top: '10px', color: 'var(--text-sub)' }} />
                <input
                  type="text"
                  placeholder="Nhập tên sản phẩm..."
                  value={keyword}
                  onChange={(e) => setKeyword(e.target.value)}
                  className="form-control"
                  style={{ paddingLeft: '2.5rem' }}
                />
              </div>
            </div>

            {/* Price Range */}
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label" style={{ fontSize: '0.8rem' }}>Khoảng giá (VND)</label>
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <input
                  type="number"
                  min="0"
                  placeholder="Từ..."
                  value={minPrice}
                  onChange={(e) => setMinPrice(e.target.value)}
                  className="form-control"
                  style={{ padding: '0.45rem 0.6rem', fontSize: '0.85rem' }}
                />
                <span>-</span>
                <input
                  type="number"
                  min="0"
                  placeholder="Đến..."
                  value={maxPrice}
                  onChange={(e) => setMaxPrice(e.target.value)}
                  className="form-control"
                  style={{ padding: '0.45rem 0.6rem', fontSize: '0.85rem' }}
                />
              </div>
            </div>

            {/* Sort Dropdown */}
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
                <option value="id,desc">Mới nhất</option>
                <option value="price,asc">Giá: Thấp đến Cao</option>
                <option value="price,desc">Giá: Cao đến Thấp</option>
                <option value="name,asc">Tên sản phẩm: A - Z</option>
              </select>
            </div>

            {/* Action Buttons */}
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>
                <Search size={16} /> Tìm kiếm
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

      {/* Product List */}
      {loading ? (
        <LoadingSpinner text="Đang tải danh sách sản phẩm..." />
      ) : products.length === 0 ? (
        <div style={{
          textAlign: 'center',
          padding: '4rem 2rem',
          background: 'white',
          borderRadius: 'var(--radius-lg)',
          border: '1px solid var(--border-color)',
          color: 'var(--text-muted)'
        }}>
          <p style={{ fontSize: '1.1rem', fontWeight: 600 }}>Không tìm thấy sản phẩm nào phù hợp.</p>
        </div>
      ) : (
        <>
          <div className="grid-products">
            {products.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>

          <Pagination
            currentPage={page}
            totalPages={totalPages}
            onPageChange={(newPage) => setPage(newPage)}
          />
        </>
      )}
    </div>
  );
};

export default HomePage;
