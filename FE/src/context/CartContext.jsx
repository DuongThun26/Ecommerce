import React, { createContext, useState, useEffect } from 'react';

export const CartContext = createContext(null);

export const CartProvider = ({ children }) => {
  const [cartItems, setCartItems] = useState(() => {
    try {
      const saved = localStorage.getItem('cart');
      if (!saved) return [];
      const parsed = JSON.parse(saved);
      return parsed.map((item) => ({
        ...item,
        addedAt: item.addedAt || new Date().toISOString(),
        selected: item.selected !== undefined ? item.selected : true,
      }));
    } catch {
      return [];
    }
  });

  const [isCartOpen, setIsCartOpen] = useState(false);

  useEffect(() => {
    localStorage.setItem('cart', JSON.stringify(cartItems));
  }, [cartItems]);

  const openCart = () => setIsCartOpen(true);
  const closeCart = () => setIsCartOpen(false);
  const toggleCart = () => setIsCartOpen((prev) => !prev);

  const addToCart = (product, quantity = 1) => {
    if (product.status === 'DISCONTINUED') {
      return;
    }
    setCartItems((prevItems) => {
      const now = new Date().toISOString();
      const existing = prevItems.find((item) => item.product.id === product.id);
      if (existing) {
        return prevItems.map((item) =>
          item.product.id === product.id
            ? { ...item, quantity: item.quantity + quantity, selected: true }
            : item
        );
      }
      return [
        ...prevItems,
        {
          product,
          quantity,
          addedAt: now,
          selected: true,
        },
      ];
    });
  };

  const buyNow = (product, quantity = 1) => {
    if (product.status === 'DISCONTINUED') {
      return;
    }
    const now = new Date().toISOString();
    setCartItems((prevItems) => {
      // Unselect all other items so ONLY this specific product is ordered
      const unselectedOthers = prevItems.map((item) => ({
        ...item,
        selected: item.product.id === product.id,
      }));

      const existing = unselectedOthers.find((item) => item.product.id === product.id);
      if (existing) {
        return unselectedOthers.map((item) =>
          item.product.id === product.id
            ? { ...item, quantity, selected: true }
            : item
        );
      }

      return [
        ...unselectedOthers,
        {
          product,
          quantity,
          addedAt: now,
          selected: true,
        },
      ];
    });
  };

  const updateQuantity = (productId, quantity) => {
    if (quantity <= 0) {
      removeFromCart(productId);
      return;
    }
    setCartItems((prevItems) =>
      prevItems.map((item) =>
        item.product.id === productId ? { ...item, quantity } : item
      )
    );
  };

  const removeFromCart = (productId) => {
    setCartItems((prevItems) =>
      prevItems.filter((item) => item.product.id !== productId)
    );
  };

  const removeSelectedFromCart = () => {
    setCartItems((prevItems) => prevItems.filter((item) => !item.selected));
  };

  const clearCart = () => {
    setCartItems([]);
  };

  const toggleSelectItem = (productId) => {
    setCartItems((prevItems) =>
      prevItems.map((item) =>
        item.product.id === productId ? { ...item, selected: !item.selected } : item
      )
    );
  };

  const toggleSelectAll = (select = true) => {
    setCartItems((prevItems) =>
      prevItems.map((item) => ({ ...item, selected: select }))
    );
  };

  const toggleSelectGroup = (dateKey, select = true) => {
    setCartItems((prevItems) =>
      prevItems.map((item) => {
        const itemDateKey = (item.addedAt || '').split('T')[0];
        return itemDateKey === dateKey ? { ...item, selected: select } : item;
      })
    );
  };

  const selectedItems = cartItems.filter((item) => item.selected);

  const totalAmount = cartItems.reduce(
    (sum, item) => sum + (item.product.price || 0) * item.quantity,
    0
  );

  const selectedTotalAmount = selectedItems.reduce(
    (sum, item) => sum + (item.product.price || 0) * item.quantity,
    0
  );

  const totalCount = cartItems.reduce((sum, item) => sum + item.quantity, 0);
  const selectedCount = selectedItems.reduce((sum, item) => sum + item.quantity, 0);

  const isAllSelected = cartItems.length > 0 && cartItems.every((item) => item.selected);

  return (
    <CartContext.Provider
      value={{
        cartItems,
        selectedItems,
        addToCart,
        buyNow,
        updateQuantity,
        removeFromCart,
        removeSelectedFromCart,
        clearCart,
        toggleSelectItem,
        toggleSelectAll,
        toggleSelectGroup,
        isAllSelected,
        totalAmount,
        selectedTotalAmount,
        totalCount,
        selectedCount,
        isCartOpen,
        setIsCartOpen,
        openCart,
        closeCart,
        toggleCart,
      }}
    >
      {children}
    </CartContext.Provider>
  );
};
