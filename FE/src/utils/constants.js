export const ROLES = {
  ADMIN: 'ADMIN',
  PRODUCT_OWNER: 'PRODUCT_OWNER',
  CUSTOMER: 'CUSTOMER',
};

export const ORDER_STATUS = {
  DRAFT: 'DRAFT',
  PENDING_APPROVAL: 'PENDING_APPROVAL',
  RETURNED: 'RETURNED',
  REJECTED: 'REJECTED',
  PAYMENT_PENDING: 'PAYMENT_PENDING',
  PAID: 'PAID',
  PROCESSING: 'PROCESSING',
  SHIPPED: 'SHIPPED',
  DELIVERED: 'DELIVERED',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED',
};

export const ORDER_STATUS_LABELS = {
  DRAFT: 'Bản nháp',
  PENDING_APPROVAL: 'Chờ PO duyệt',
  RETURNED: 'Yêu cầu sửa đổi',
  REJECTED: 'Từ chối duyệt',
  PAYMENT_PENDING: 'Chờ thanh toán',
  PAID: 'Đã thanh toán',
  PROCESSING: 'Đang xử lý',
  SHIPPED: 'Đang giao hàng',
  DELIVERED: 'Đã giao hàng',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
};

export const ORDER_STATUS_BADGE = {
  DRAFT: 'neutral',
  PENDING_APPROVAL: 'warning',
  RETURNED: 'info',
  REJECTED: 'danger',
  PAYMENT_PENDING: 'warning',
  PAID: 'success',
  PROCESSING: 'info',
  SHIPPED: 'info',
  DELIVERED: 'success',
  COMPLETED: 'success',
  CANCELLED: 'danger',
};

export const TICKET_STATUS = {
  PENDING_APPROVAL: 'PENDING_APPROVAL',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  RETURNED: 'RETURNED',
};

export const TICKET_STATUS_LABELS = {
  PENDING_APPROVAL: 'Chờ PO duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
  RETURNED: 'Trả lại bổ sung',
};

export const INVOICE_STATUS = {
  ISSUED: 'ISSUED',
  CANCELLED: 'CANCELLED',
};

export const INVOICE_STATUS_LABELS = {
  ISSUED: 'Đã phát hành',
  CANCELLED: 'Đã hủy',
};

export const PAYMENT_METHODS = {
  VNPAY: 'VNPAY',
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
};
