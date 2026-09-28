import React, { useState, useEffect, useRef } from 'react';
import Modal from '../common/Modal';
import productService from '../../services/product.service';
import { UploadCloud, X, Image as ImageIcon, CheckCircle, Loader2, Plus } from 'lucide-react';

export const ProductModal = ({ isOpen, onClose, onSave, product = null, loading = false }) => {
  const [formData, setFormData] = useState({
    name: '',
    price: '',
    stock: '',
    status: 'AVAILABLE',
  });

  const [imageUrls, setImageUrls] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [uploadProgressText, setUploadProgressText] = useState('');
  const fileInputRef = useRef(null);

  useEffect(() => {
    if (product) {
      setFormData({
        name: product.name || '',
        price: product.price || '',
        stock: product.stock !== undefined ? product.stock : '',
        status: product.status || 'AVAILABLE',
      });
      // Extract existing images
      const existingImages = product.imageUrls || (product.images ? product.images.map(img => img.imageUrl) : []);
      if (existingImages.length === 0 && product.primaryImageUrl) {
        existingImages.push(product.primaryImageUrl);
      }
      setImageUrls(existingImages);
    } else {
      setFormData({
        name: '',
        price: '',
        stock: '',
        status: 'AVAILABLE',
      });
      setImageUrls([]);
    }
    setUploading(false);
    setUploadProgressText('');
  }, [product, isOpen]);

  const handleFilesSelected = async (e) => {
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;

    setUploading(true);
    setUploadProgressText(`Đang tải ${files.length} ảnh lên Cloudinary...`);
    try {
      const uploadedUrls = await productService.uploadMultipleImages(files);
      if (uploadedUrls && uploadedUrls.length > 0) {
        setImageUrls(prev => [...prev, ...uploadedUrls]);
      }
    } catch (err) {
      console.error('Upload to Cloudinary failed:', err);
      alert('Tải ảnh lên Cloudinary thất bại. Vui lòng kiểm tra lại!');
    } finally {
      setUploading(false);
      setUploadProgressText('');
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleRemoveImage = (indexToRemove) => {
    setImageUrls(prev => prev.filter((_, idx) => idx !== indexToRemove));
  };

  const handleSetPrimaryImage = (index) => {
    if (index === 0) return;
    setImageUrls(prev => {
      const copy = [...prev];
      const selected = copy.splice(index, 1)[0];
      return [selected, ...copy];
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave({
      name: formData.name.trim(),
      price: Number(formData.price),
      stock: Number(formData.stock),
      status: formData.status || 'AVAILABLE',
      imageUrls: imageUrls,
    });
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={product ? 'Chỉnh sửa sản phẩm' : 'Thêm sản phẩm mới'}
      footer={
        <>
          <button type="button" onClick={onClose} className="btn btn-secondary" disabled={uploading || loading}>
            Hủy
          </button>
          <button
            type="submit"
            form="product-form"
            disabled={loading || uploading || !formData.name || formData.price === '' || formData.stock === ''}
            className="btn btn-primary"
          >
            {loading ? 'Đang lưu...' : uploading ? 'Đang tải ảnh...' : 'Lưu sản phẩm'}
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
            <option value="AVAILABLE">AVAILABLE (Đang kinh doanh)</option>
            <option value="OUT_OF_STOCK">OUT_OF_STOCK (Tạm hết hàng)</option>
            <option value="DISCONTINUED">DISCONTINUED (Ngừng kinh doanh)</option>
          </select>
        </div>

        {/* Cloudinary Image Upload Section */}
        <div className="form-group" style={{ marginTop: '1.25rem' }}>
          <label className="form-label" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>Hình ảnh sản phẩm (Cloudinary)</span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 'normal' }}>
              {imageUrls.length} ảnh đã chọn
            </span>
          </label>

          {/* Hidden File Input */}
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFilesSelected}
            multiple
            accept="image/*"
            style={{ display: 'none' }}
          />

          {/* Upload Dropzone / Trigger */}
          <div
            onClick={() => !uploading && fileInputRef.current?.click()}
            style={{
              border: '2px dashed #cbd5e1',
              borderRadius: 'var(--radius-md)',
              padding: '1.5rem',
              textAlign: 'center',
              backgroundColor: '#f8fafc',
              cursor: uploading ? 'not-allowed' : 'pointer',
              transition: 'all 0.2s ease',
              marginBottom: '1rem',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.borderColor = 'var(--color-primary)'; e.currentTarget.style.backgroundColor = '#eff6ff'; }}
            onMouseLeave={(e) => { e.currentTarget.style.borderColor = '#cbd5e1'; e.currentTarget.style.backgroundColor = '#f8fafc'; }}
          >
            {uploading ? (
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.5rem', color: 'var(--color-primary)' }}>
                <Loader2 size={32} className="animate-spin" />
                <span style={{ fontSize: '0.9rem', fontWeight: 600 }}>{uploadProgressText}</span>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.5rem', color: '#64748b' }}>
                <UploadCloud size={32} color="var(--color-primary)" />
                <span style={{ fontSize: '0.95rem', fontWeight: 600, color: '#334155' }}>
                  Nhấn để chọn ảnh hoặc kéo thả vào đây (1 hoặc nhiều ảnh)
                </span>
                <span style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
                  Hỗ trợ định dạng JPG, PNG, WEBP, GIF (Tự động tải lên Cloudinary)
                </span>
              </div>
            )}
          </div>

          {/* Image Previews Grid */}
          {imageUrls.length > 0 && (
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(90px, 1fr))',
              gap: '0.75rem',
              marginTop: '0.5rem'
            }}>
              {imageUrls.map((url, idx) => (
                <div
                  key={idx}
                  style={{
                    position: 'relative',
                    aspectRatio: '1',
                    borderRadius: 'var(--radius-sm)',
                    overflow: 'hidden',
                    border: idx === 0 ? '2px solid var(--color-primary)' : '1px solid var(--border-color)',
                    boxShadow: 'var(--shadow-sm)',
                    backgroundColor: '#f1f5f9'
                  }}
                  title={idx === 0 ? 'Ảnh đại diện (Primary)' : 'Click để đặt làm ảnh đại diện'}
                  onClick={() => handleSetPrimaryImage(idx)}
                >
                  <img
                    src={url}
                    alt={`Preview ${idx + 1}`}
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />

                  {idx === 0 && (
                    <div style={{
                      position: 'absolute',
                      bottom: 0,
                      left: 0,
                      right: 0,
                      backgroundColor: 'rgba(37, 99, 235, 0.85)',
                      color: 'white',
                      fontSize: '0.65rem',
                      fontWeight: 700,
                      textAlign: 'center',
                      padding: '2px 0',
                    }}>
                      ẢNH CHÍNH
                    </div>
                  )}

                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleRemoveImage(idx);
                    }}
                    style={{
                      position: 'absolute',
                      top: '4px',
                      right: '4px',
                      backgroundColor: 'rgba(0, 0, 0, 0.65)',
                      color: 'white',
                      border: 'none',
                      borderRadius: '50%',
                      width: '20px',
                      height: '20px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      cursor: 'pointer',
                      padding: 0,
                    }}
                    title="Xóa ảnh này"
                  >
                    <X size={12} />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </form>
    </Modal>
  );
};

export default ProductModal;
