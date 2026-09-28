import React, { useState, useEffect } from 'react';
import productService from '../../services/product.service';
import ProductCard from '../../components/customer/ProductCard';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Pagination from '../../components/common/Pagination';
import { Search, SlidersHorizontal, Sparkles } from 'lucide-react';

export const HomePage = () => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [sortBy, setSortBy] = useState('id,desc');

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const res = await productService.getProducts({
        page,
        size: 8,
        keyword: keyword.trim() || undefined,
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
      <div style={{
        display: 'flex',
        flexWrap: 'wrap',
        justifyContent: 'space-between',
        alignItems: 'center',
        gap: '1rem',
        marginBottom: '2rem',
      }}>
        <form onSubmit={handleSearch} style={{ display: 'flex', gap: '0.5rem', flex: 1, maxWidth: '480px' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <Search size={18} style={{ position: 'absolute', left: '12px', top: '12px', color: 'var(--text-sub)' }} />
            <input
              type="text"
              placeholder="Tìm kiếm sản phẩm theo tên..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              className="form-control"
              style={{ paddingLeft: '2.5rem' }}
            />
          </div>
          <button type="submit" className="btn btn-primary">
            Tìm kiếm
          </button>
        </form>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <SlidersHorizontal size={18} color="var(--text-muted)" />
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="form-select"
            style={{ width: 'auto' }}
          >
            <option value="id,desc">Mới nhất</option>
            <option value="price,asc">Giá: Thấp đến Cao</option>
            <option value="price,desc">Giá: Cao đến Thấp</option>
            <option value="name,asc">Tên: A - Z</option>
          </select>
        </div>
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
