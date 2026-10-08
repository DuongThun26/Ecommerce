package com.example.ecommerce.mail.scheduler;

import com.example.ecommerce.mail.entity.EmailLogEntity;
import com.example.ecommerce.mail.enums.EmailStatus;
import com.example.ecommerce.mail.repository.EmailLogRepository;
import com.example.ecommerce.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailRetryScheduler {

    private final EmailLogRepository emailLogRepository;
    private final EmailService emailService;

    /**
     * Tác vụ định kỳ quét hàng đợi email mỗi 5 giây.
     * Tìm các email ở trạng thái PENDING hoặc RETRYING mà đã đến thời điểm nextRetryAt để xử lý gửi lại.
     */
    @Scheduled(fixedDelayString = "${app.mail.retry-poll-interval-ms:5000}")
    public void processPendingAndRetryEmails() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<EmailStatus> targetStatuses = List.of(EmailStatus.PENDING, EmailStatus.RETRYING);
            List<EmailLogEntity> readyEmails = emailLogRepository.findReadyToProcessEmails(
                    targetStatuses,
                    now,
                    PageRequest.of(0, 20)
            );

            if (!readyEmails.isEmpty()) {
                log.debug("Found {} email(s) ready for delivery/retry.", readyEmails.size());
            }

            for (EmailLogEntity emailLog : readyEmails) {
                try {
                    emailService.processEmailLog(emailLog.getId());
                } catch (Exception ex) {
                    log.error("Unexpected error in EmailRetryScheduler for EmailLog #{}: {}", emailLog.getId(), ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.error("Error running EmailRetryScheduler: {}", ex.getMessage(), ex);
        }
    }
}
