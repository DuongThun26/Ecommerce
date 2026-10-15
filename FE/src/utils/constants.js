export const ROLES = {
  ADMIN: 'ADMIN',
  PRODUCT_OWNER: 'PRODUCT_OWNER',
  CUSTOMER: 'CUSTOMER',
};

export const ORDER_STATUS = {
  DRAFT: 'DRAFT',
  PENDING_APPROVAL: 'PENDING_APPROVAL',
  PENDING_PO_REVIEW: 'PENDING_PO_REVIEW',
  ACTION_REQUIRED: 'ACTION_REQUIRED',
  RETURNED: 'RETURNED',
  REJECTED: 'REJECTED',
  AWAITING_PAYMENT: 'AWAITING_PAYMENT',
  PAID: 'PAID',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED',
};

export const ORDER_STATUS_LABELS = {
  DRAFT: 'Bản nháp',
  PENDING_APPROVAL: 'Chờ PO duyệt',
  PENDING_PO_REVIEW: 'Chờ các PO duyệt',
  ACTION_REQUIRED: 'Cần khách hàng xác nhận',
  RETURNED: 'Yêu cầu sửa đổi',
  REJECTED: 'Từ chối duyệt',
  AWAITING_PAYMENT: 'Chờ thanh toán',
  PAID: 'Đã thanh toán',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
};

export const ORDER_STATUS_BADGE = {
  DRAFT: 'neutral',
  PENDING_APPROVAL: 'warning',
  PENDING_PO_REVIEW: 'warning',
  ACTION_REQUIRED: 'danger',
  RETURNED: 'info',
  REJECTED: 'danger',
  AWAITING_PAYMENT: 'warning',
  PAID: 'success',
  COMPLETED: 'success',
  CANCELLED: 'danger',
};

export const TICKET_STATUS = {
  PENDING_APPROVAL: 'PENDING_APPROVAL',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  RETURNED: 'RETURNED',
  CANCELLED: 'CANCELLED',
};

export const TICKET_STATUS_LABELS = {
  PENDING_APPROVAL: 'Chờ PO duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
  RETURNED: 'Trả lại bổ sung',
  CANCELLED: 'Đã hủy',
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
