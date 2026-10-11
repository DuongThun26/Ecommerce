package com.example.ecommerce.mail.service.impl;

import com.example.ecommerce.common.exception.EmailNotResendableException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.mail.dto.response.EmailOutboxResponse;
import com.example.ecommerce.mail.entity.EmailOutboxEntity;
import com.example.ecommerce.mail.enums.EmailOutboxStatus;
import com.example.ecommerce.mail.repository.EmailOutboxRepository;
import com.example.ecommerce.mail.service.EmailOutboxService;
import com.example.ecommerce.order.entity.OrderEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailOutboxServiceImpl implements EmailOutboxService {

    private final EmailOutboxRepository emailOutboxRepository;

    @Value("${app.demo.fail-after-paid:false}")
    private boolean failAfterPaid;

    @Override
    @Transactional
    public void recordOrderPaidOutbox(OrderEntity order) {
        if (order == null || order.getId() == null) {
            log.warn("Cannot record outbox: Order is null or id is missing");
            return;
        }

        // Đảm bảo tính Idempotent: Nếu đã có dòng outbox cho ORDER_PAID và đơn hàng này thì bỏ qua
        if (emailOutboxRepository.findByTemplateAndReferenceId("ORDER_PAID", order.getId()).isPresent()) {
            log.info("Email outbox for ORDER_PAID order #{} already exists. Skipping duplicate insert.", order.getId());
            return;
        }

        String recipientEmail = null;
        if (order.getCustomer() != null && order.getCustomer().getEmail() != null && !order.getCustomer().getEmail().isBlank()) {
            recipientEmail = order.getCustomer().getEmail();
        } else if (order.getCustomer() != null && order.getCustomer().getUser() != null && order.getCustomer().getUser().getEmail() != null) {
            recipientEmail = order.getCustomer().getUser().getEmail();
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            recipientEmail = "customer" + order.getId() + "@example.com";
        }

        EmailOutboxEntity outbox = EmailOutboxEntity.builder()
                .template("ORDER_PAID")
                .referenceId(order.getId())
                .recipient(recipientEmail.trim())
                .status(EmailOutboxStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        emailOutboxRepository.save(outbox);
        log.info("Recorded transactional email outbox id: {} for order #{} in status PENDING.", outbox.getId(), order.getId());

        // Quy tắc 11: Nếu profile demo bật fail-after-paid=true, ném exception sau khi ghi outbox trong cùng transaction
        if (failAfterPaid) {
            log.error("Simulation trigger: app.demo.fail-after-paid is true. Throwing exception to rollback transaction.");
            throw new RuntimeException("Simulated exception after outbox: app.demo.fail-after-paid=true");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailOutboxResponse> getEmailOutboxList(EmailOutboxStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        if (status != null) {
            return emailOutboxRepository.findAllByStatus(status, pageable)
                    .map(EmailOutboxResponse::fromEntity);
        }
        return emailOutboxRepository.findAll(pageable)
                .map(EmailOutboxResponse::fromEntity);
    }

    @Override
    @Transactional
    public EmailOutboxResponse resendFailedEmail(Long id) {
        EmailOutboxEntity outbox = emailOutboxRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Email outbox với ID " + id + " không tồn tại."));

        // Quy tắc 10: Nếu mail không ở FAILED, hệ thống trả 409 EMAIL_NOT_RESENDABLE
        if (outbox.getStatus() != EmailOutboxStatus.FAILED) {
            throw new EmailNotResendableException(
                    "Email #" + id + " đang ở trạng thái " + outbox.getStatus() + ", chỉ có thể gửi lại khi ở trạng thái FAILED."
            );
        }

        outbox.setStatus(EmailOutboxStatus.PENDING);
        outbox.setAttempts(0);
        outbox.setNextAttemptAt(LocalDateTime.now());
        outbox.setLastError(null);

        EmailOutboxEntity updated = emailOutboxRepository.save(outbox);
        log.info("Resend requested for email outbox #{}. Reset status to PENDING, attempts to 0.", id);
        return EmailOutboxResponse.fromEntity(updated);
    }
}
