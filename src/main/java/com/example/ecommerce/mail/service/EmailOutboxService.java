package com.example.ecommerce.mail.service;

import com.example.ecommerce.mail.dto.response.EmailOutboxResponse;
import com.example.ecommerce.mail.enums.EmailOutboxStatus;
import com.example.ecommerce.order.entity.OrderEntity;
import org.springframework.data.domain.Page;

public interface EmailOutboxService {

    /**
     * Ghi 1 bản ghi vào EMAIL_OUTBOX khi đơn hàng chuyển sang trạng thái PAID (trong cùng transaction nghiệp vụ).
     * Tuyệt đối không gọi SMTP tại đây.
     */
    void recordOrderPaidOutbox(OrderEntity order);

    /**
     * Lấy danh sách email outbox theo status (có phân trang).
     */
    Page<EmailOutboxResponse> getEmailOutboxList(EmailOutboxStatus status, int page, int size);

    /**
     * Đặt lại trạng thái PENDING để gửi lại một email đang ở trạng thái FAILED.
     * Nếu email không ở FAILED, ném EmailNotResendableException (HTTP 409).
     */
    EmailOutboxResponse resendFailedEmail(Long id);
}
