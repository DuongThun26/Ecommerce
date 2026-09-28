import React, { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../../hooks/useCart';
import { formatCurrency } from '../../utils/formatters';
import { X, Trash2, Plus, Minus, ArrowRight, ShoppingBag, Calendar, CheckSquare, Square } from 'lucide-react';

export const CartDrawer = ({ isOpen, onClose }) => {
  const {
    cartItems,
    selectedItems,
    updateQuantity,
    removeFromCart,
    selectedTotalAmount,
    selectedCount,
    totalCount,
    toggleSelectItem,
    toggleSelectAll,
    toggleSelectGroup,
    isAllSelected,
    isCartOpen,
    closeCart,
  } = useCart();
  const navigate = useNavigate();

  const isDrawerOpen = isOpen !== undefined ? isOpen : isCartOpen;
  const handleClose = onClose || closeCart;

  // Group cart items by added date
  const groupedCartItems = useMemo(() => {
    const groups = {};
    cartItems.forEach((item) => {
      const dateObj = item.addedAt ? new Date(item.addedAt) : new Date();
      const dateKey = !isNaN(dateObj.getTime()) ? dateObj.toISOString().split('T')[0] : 'other';
      const formattedDate = !isNaN(dateObj.getTime())
        ? dateObj.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
        : 'Sản phẩm khác';

      if (!groups[dateKey]) {
        groups[dateKey] = {
          dateKey,
          label: formattedDate,
          items: [],
        };
      }
      groups[dateKey].items.push(item);
    });
    return Object.values(groups);
  }, [cartItems]);

  if (!isDrawerOpen) return null;

  const handleCheckout = () => {
    if (selectedCount === 0) return;
    handleClose();
    navigate('/checkout');
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.5)',
        backdropFilter: 'blur(3px)',
        zIndex: 2000,
        display: 'flex',
        justifyContent: 'flex-end',
      }}
      onClick={handleClose}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '460px',
          background: 'white',
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: 'var(--shadow-xl)',
          animation: 'slideLeft 0.25s ease-out',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div style={{
          padding: '1.25rem 1.5rem',
          borderBottom: '1px solid var(--border-color)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <ShoppingBag size={20} color="var(--color-primary)" />
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700 }}>Giỏ hàng ({totalCount})</h3>
          </div>
          <button
            onClick={handleClose}
            className="btn-outline btn-icon"
            style={{ border: 'none', background: 'transparent' }}
          >
            <X size={20} />
          </button>
        </div>

        {/* Select All Bar */}
        {cartItems.length > 0 && (
          <div style={{
            padding: '0.75rem 1.5rem',
            background: '#f8fafc',
            borderBottom: '1px solid var(--border-color)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.875rem', fontWeight: 600 }}>
              <input
                type="checkbox"
                checked={isAllSelected}
                onChange={(e) => toggleSelectAll(e.target.checked)}
                style={{ width: '16px', height: '16px', cursor: 'pointer' }}
              />
              Chọn tất cả ({cartItems.length} loại sp)
            </label>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Đã chọn: <strong style={{ color: 'var(--color-primary)' }}>{selectedCount}</strong> sp
            </span>
          </div>
        )}

        {/* Cart Items List Grouped by Date */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '1.25rem 1.5rem', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {cartItems.length === 0 ? (
            <div style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              height: '100%',
              color: 'var(--text-muted)',
              gap: '1rem',
            }}>
              <ShoppingBag size={48} strokeWidth={1.5} />
              <p style={{ fontWeight: 600 }}>Giỏ hàng hiện đang trống</p>
            </div>
          ) : (
            groupedCartItems.map((group) => {
              const groupAllSelected = group.items.every((item) => item.selected);
              return (
                <div key={group.dateKey} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {/* Date Group Header */}
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '0.4rem 0.6rem',
                    background: '#f1f5f9',
                    borderRadius: 'var(--radius-sm)',
                  }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', cursor: 'pointer', fontSize: '0.825rem', fontWeight: 700, color: 'var(--text-main)' }}>
                      <input
                        type="checkbox"
                        checked={groupAllSelected}
                        onChange={(e) => toggleSelectGroup(group.dateKey, e.target.checked)}
                        style={{ width: '15px', height: '15px', cursor: 'pointer' }}
                      />
                      <Calendar size={14} color="var(--color-primary)" />
                      Ngày thêm: {group.label}
                    </label>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600 }}>
                      {group.items.length} sp
                    </span>
                  </div>

                  {/* Group Items */}
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                    {group.items.map(({ product, quantity, selected }) => (
                      <div
                        key={product.id}
                        style={{
                          display: 'flex',
                          gap: '0.75rem',
                          padding: '0.875rem',
                          borderRadius: 'var(--radius-md)',
                          background: selected ? 'white' : '#f8fafc',
                          border: selected ? '1.5px solid var(--color-primary)' : '1px solid var(--border-color)',
                          boxShadow: selected ? '0 2px 8px rgba(79, 70, 229, 0.08)' : 'none',
                          alignItems: 'center',
                          transition: 'all 0.2s ease',
                        }}
                      >
                        {/* Checkbox per item */}
                        <input
                          type="checkbox"
                          checked={selected}
                          onChange={() => toggleSelectItem(product.id)}
                          style={{ width: '18px', height: '18px', cursor: 'pointer', flexShrink: 0 }}
                          title="Chọn đặt sản phẩm này"
                        />

                        {/* Thumbnail */}
                        <div style={{
                          width: '52px',
                          height: '52px',
                          background: '#f1f5f9',
                          borderRadius: 'var(--radius-sm)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontSize: '1.4rem',
                          border: '1px solid var(--border-color)',
                          flexShrink: 0,
                        }}>
                          📦
                        </div>

                        {/* Info & Quantity Actions */}
                        <div style={{ flex: 1, minWidth: 0 }}>
                          <h5 style={{
                            fontSize: '0.875rem',
                            fontWeight: 700,
                            marginBottom: '0.2rem',
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                          }}>
                            {product.name}
                          </h5>
                          <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--color-primary)', marginBottom: '0.4rem' }}>
                            {formatCurrency(product.price)}
                          </div>

                          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', background: 'white', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-sm)', padding: '0.1rem' }}>
                              <button
                                onClick={() => updateQuantity(product.id, quantity - 1)}
                                style={{ border: 'none', background: 'transparent', cursor: 'pointer', padding: '0.2rem', display: 'flex' }}
                                title="Giảm"
                              >
                                <Minus size={13} />
                              </button>
                              <span style={{ fontSize: '0.825rem', fontWeight: 700, minWidth: '20px', textAlign: 'center' }}>
                                {quantity}
                              </span>
                              <button
                                onClick={() => updateQuantity(product.id, quantity + 1)}
                                style={{ border: 'none', background: 'transparent', cursor: 'pointer', padding: '0.2rem', display: 'flex' }}
                                title="Tăng"
                              >
                                <Plus size={13} />
                              </button>
                            </div>

                            <button
                              onClick={() => removeFromCart(product.id)}
                              style={{ border: 'none', background: 'transparent', cursor: 'pointer', color: 'var(--color-danger)', padding: '0.25rem' }}
                              title="Xóa khỏi giỏ"
                            >
                              <Trash2 size={16} />
                            </button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Footer with Selected Total & Checkout */}
        {cartItems.length > 0 && (
          <div style={{
            padding: '1.25rem 1.5rem',
            borderTop: '1px solid var(--border-color)',
            background: '#f8fafc',
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <span style={{ fontWeight: 600, color: 'var(--text-muted)', fontSize: '0.85rem' }}>Tổng thanh toán:</span>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  ({selectedCount} sản phẩm được chọn)
                </div>
              </div>
              <span style={{ fontSize: '1.3rem', fontWeight: 800, color: 'var(--color-primary)' }}>
                {formatCurrency(selectedTotalAmount)}
              </span>
            </div>

            <button
              onClick={handleCheckout}
              disabled={selectedCount === 0}
              className={`btn btn-lg ${selectedCount > 0 ? 'btn-primary' : 'btn-secondary'}`}
              style={{ width: '100%' }}
            >
              {selectedCount > 0 ? (
                <>
                  Đặt hàng ({selectedCount} sp) <ArrowRight size={18} />
                </>
              ) : (
                'Vui lòng chọn sản phẩm muốn đặt'
              )}
            </button>
          </div>
        )}
      </div>

      <style>{`
        @keyframes slideLeft {
          from { transform: translateX(100%); }
          to { transform: translateX(0); }
        }
      `}</style>
    </div>
  );
};

export default CartDrawer;
