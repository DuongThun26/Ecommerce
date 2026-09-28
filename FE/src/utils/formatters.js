/**
 * Format number into Vietnamese Dong currency string
 * e.g., 25000000 -> 25.000.000 ₫
 */
export const formatCurrency = (amount) => {
  if (amount === null || amount === undefined || isNaN(amount)) {
    return '0 ₫';
  }
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(amount);
};

const parseDateInput = (val) => {
  if (!val) return null;
  if (Array.isArray(val)) {
    // [year, month, day, hour, minute, second]
    const [year, month, day = 1, hour = 0, minute = 0, second = 0] = val;
    return new Date(year, month - 1, day, hour, minute, second);
  }
  const date = new Date(val);
  return isNaN(date.getTime()) ? null : date;
};

/**
 * Format datetime string
 * e.g., 2026-09-24T14:00:00 -> 24/09/2026 14:00
 */
export const formatDateTime = (dateStr) => {
  if (!dateStr) return 'N/A';
  try {
    const date = parseDateInput(dateStr);
    if (!date) return 'N/A';
    return new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  } catch {
    return 'N/A';
  }
};

/**
 * Format date only
 */
export const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A';
  try {
    const date = parseDateInput(dateStr);
    if (!date) return 'N/A';
    return new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(date);
  } catch {
    return 'N/A';
  }
};
