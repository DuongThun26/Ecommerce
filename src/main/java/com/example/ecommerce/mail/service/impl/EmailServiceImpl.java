package com.example.ecommerce.mail.service.impl;

import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.invoice.entity.InvoiceEntity;
import com.example.ecommerce.invoice.entity.InvoiceItemEntity;
import com.example.ecommerce.invoice.service.PdfStorageService;
import com.example.ecommerce.mail.dto.response.EmailLogResponse;
import com.example.ecommerce.mail.entity.EmailLogEntity;
import com.example.ecommerce.mail.enums.EmailStatus;
import com.example.ecommerce.mail.mapper.EmailLogMapper;
import com.example.ecommerce.mail.repository.EmailLogRepository;
import com.example.ecommerce.mail.service.EmailService;
import com.example.ecommerce.mail.specification.EmailLogSpecification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat CURRENCY_FORMAT;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        CURRENCY_FORMAT = new DecimalFormat("#,##0 VND", symbols);
    }

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final EmailLogMapper emailLogMapper;
    private final PdfStorageService pdfStorageService;

    @Value("${spring.mail.username:}")
    private String mailUsername = "";

    @Value("${app.invoice.store-name:E-COMMERCE STORE}")
    private String storeName = "E-COMMERCE STORE";

    @Value("${app.invoice.store-email:support@ecommerce.local}")
    private String storeEmail = "support@ecommerce.local";

    @Value("${app.invoice.store-phone:+84 976 570 703}")
    private String storePhone = "+84 976 570 703";

    @Value("${app.invoice.store-address:Tân Mỹ, Nam Từ Liêm, TP Hà Nội}")
    private String storeAddress = "Tân Mỹ, Nam Từ Liêm, TP Hà Nội";

    @Override
    public void sendEmail(String to, String subject, String contentHtml) {
        queueEmail(to, subject, contentHtml, null, null, null, null);
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String contentHtml, String attachmentFileName, byte[] attachmentBytes, String contentType) {
        String storageKey = null;
        if (attachmentBytes != null && attachmentBytes.length > 0 && attachmentFileName != null) {
            String sanitizedName = attachmentFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            storageKey = "mail_attachments/" + System.currentTimeMillis() + "_" + sanitizedName;
            try {
                pdfStorageService.store(storageKey, attachmentBytes);
            } catch (Exception ex) {
                log.error("Failed to store mail attachment in pdf storage service, proceeding with queue: {}", ex.getMessage());
            }
        }
        queueEmail(to, subject, contentHtml, attachmentFileName, storageKey, contentType, null);
    }

    @Override
    public void sendInvoiceEmail(InvoiceEntity invoice, byte[] invoicePdfBytes) {
        if (invoice == null) {
            log.warn("Cannot send invoice email: invoice is null.");
            return;
        }

        String recipientEmail = invoice.getBillingEmail();
        String subject = String.format("[%s] Xác nhận đơn hàng & Hóa đơn điện tử #%s", storeName, invoice.getInvoiceNumber());
        String emailContent = buildInvoiceEmailHtml(invoice);
        String attachmentFileName = String.format("HoaDon_%s.pdf", invoice.getInvoiceNumber());

        String storageKey = invoice.getPdfStorageKey();
        if ((storageKey == null || !pdfStorageService.exists(storageKey)) && invoicePdfBytes != null && invoicePdfBytes.length > 0) {
            storageKey = "invoices/HoaDon_" + invoice.getInvoiceNumber() + ".pdf";
            try {
                pdfStorageService.store(storageKey, invoicePdfBytes);
            } catch (Exception ex) {
                log.error("Failed to store invoice PDF for email: {}", ex.getMessage());
            }
        }

        String metadata = "{\"invoiceId\": " + invoice.getId() + ", \"invoiceNumber\": \"" + invoice.getInvoiceNumber() + "\"}";
        queueEmail(recipientEmail, subject, emailContent, attachmentFileName, storageKey, "application/pdf", metadata);
    }

    /**
     * Đưa email vào hàng đợi EmailLog với trạng thái PENDING và kích hoạt gửi ngay bất đồng bộ.
     * Hoạt động trong transaction riêng biệt (REQUIRES_NEW) để không bao giờ ảnh hưởng đến transaction đơn hàng.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailLogEntity queueEmail(String to, String subject, String contentHtml, String attachmentFileName, String attachmentStorageKey, String attachmentContentType, String metadata) {
        EmailLogEntity emailLog = EmailLogEntity.builder()
                .recipientEmail(to != null ? to.trim() : "")
                .subject(subject != null ? subject.trim() : "")
                .contentHtml(contentHtml != null ? contentHtml : "")
                .attachmentFileName(attachmentFileName)
                .attachmentStorageKey(attachmentStorageKey)
                .attachmentContentType(attachmentContentType)
                .status(EmailStatus.PENDING)
                .attemptCount(0)
                .maxAttempts(4)
                .nextRetryAt(LocalDateTime.now())
                .metadataInfo(metadata)
                .build();

        EmailLogEntity saved = emailLogRepository.save(emailLog);
        log.info("Enqueued EmailLog #{} for recipient '{}' with subject: '{}'", saved.getId(), saved.getRecipientEmail(), saved.getSubject());

        // Kích hoạt gửi ngay lập tức ở background (nếu email hợp lệ)
        dispatchEmailAsync(saved.getId());
        return saved;
    }

    @Async
    public void dispatchEmailAsync(Long emailLogId) {
        try {
            processEmailLog(emailLogId);
        } catch (Exception ex) {
            log.error("Async email dispatch error for EmailLog #{}: {}", emailLogId, ex.getMessage());
        }
    }

    /**
     * Xử lý gửi 1 bản ghi emailLog. Chạy trong transaction riêng biệt (REQUIRES_NEW)
     * Đảm bảo kiểm tra tính hợp lệ email, retry theo đúng chu kỳ backoff và đẩy vào Dead Letter Queue khi cần.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEmailLog(Long emailLogId) {
        EmailLogEntity emailLog = emailLogRepository.findById(emailLogId).orElse(null);
        if (emailLog == null) {
            log.warn("EmailLog with id {} not found for processing.", emailLogId);
            return;
        }

        // Bỏ qua nếu đã hoàn tất hoặc đã vào Dead Letter Queue
        if (emailLog.getStatus() == EmailStatus.SENT || emailLog.getStatus() == EmailStatus.DEAD_LETTER) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        emailLog.setLastAttemptAt(now);
        emailLog.setStatus(EmailStatus.PROCESSING);

        // 1. KIỂM TRA ĐỊA CHỈ EMAIL NGƯỜI NHẬN
        String recipient = emailLog.getRecipientEmail();
        if (!isValidEmail(recipient)) {
            emailLog.setStatus(EmailStatus.DEAD_LETTER);
            emailLog.setErrorType("INVALID_RECIPIENT_EMAIL");
            emailLog.setLastErrorMessage("Địa chỉ email người nhận không đúng hoặc không hợp lệ: '" + recipient + "'");
            emailLog.setAttemptCount(emailLog.getAttemptCount() + 1);
            emailLog.setNextRetryAt(null);
            emailLogRepository.save(emailLog);
            log.error("EmailLog #{} recipient '{}' is INVALID. Moved immediately to DEAD LETTER QUEUE.", emailLog.getId(), recipient);
            return;
        }

        // 2. THỰC HIỆN GỬI EMAIL THỰC TẾ QUA JavaMailSender
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            boolean hasAttachment = emailLog.getAttachmentStorageKey() != null && !emailLog.getAttachmentStorageKey().isBlank();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, hasAttachment, "UTF-8");

            String sender = (mailUsername != null && !mailUsername.isBlank()) ? mailUsername : ((storeEmail != null && !storeEmail.isBlank()) ? storeEmail : "support@ecommerce.local");
            String fromName = (storeName != null && !storeName.isBlank()) ? storeName : "E-COMMERCE STORE";
            helper.setFrom(sender, fromName);
            helper.setTo(recipient);
            helper.setSubject(emailLog.getSubject());
            helper.setText(emailLog.getContentHtml(), true);

            if (hasAttachment && pdfStorageService.exists(emailLog.getAttachmentStorageKey())) {
                byte[] attachmentBytes = pdfStorageService.load(emailLog.getAttachmentStorageKey());
                if (attachmentBytes != null && attachmentBytes.length > 0) {
                    ByteArrayResource resource = new ByteArrayResource(attachmentBytes);
                    String fileName = emailLog.getAttachmentFileName() != null ? emailLog.getAttachmentFileName() : "attachment.pdf";
                    if (emailLog.getAttachmentContentType() != null && !emailLog.getAttachmentContentType().isBlank()) {
                        helper.addAttachment(fileName, resource, emailLog.getAttachmentContentType());
                    } else {
                        helper.addAttachment(fileName, resource);
                    }
                }
            }

            mailSender.send(mimeMessage);

            // Gửi thành công!
            emailLog.setStatus(EmailStatus.SENT);
            emailLog.setSentAt(now);
            emailLog.setNextRetryAt(null);
            emailLog.setLastErrorMessage(null);
            emailLog.setErrorType(null);
            emailLogRepository.save(emailLog);
            log.info("EmailLog #{} successfully sent to '{}' on attempt #{}.", emailLog.getId(), recipient, emailLog.getAttemptCount() + 1);

        } catch (AddressException ex) {
            // Lỗi địa chỉ email không hợp lệ từ Mail Server -> Đưa thẳng vào Dead Letter Queue
            emailLog.setStatus(EmailStatus.DEAD_LETTER);
            emailLog.setErrorType("INVALID_ADDRESS_EXCEPTION");
            emailLog.setLastErrorMessage("Mail server rejected recipient address: " + ex.getMessage());
            emailLog.setAttemptCount(emailLog.getAttemptCount() + 1);
            emailLog.setNextRetryAt(null);
            emailLogRepository.save(emailLog);
            log.error("EmailLog #{} recipient '{}' rejected by server. Moved to DEAD LETTER QUEUE: {}", emailLog.getId(), recipient, ex.getMessage());

        } catch (Exception ex) {
            // Lỗi kết nối / timeout / SMTP tạm thời -> Xử lý theo quy trình Retry 4 bước
            int currentAttempt = emailLog.getAttemptCount() + 1;
            emailLog.setAttemptCount(currentAttempt);
            String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            emailLog.setLastErrorMessage(errorMsg);

            if (currentAttempt >= emailLog.getMaxAttempts()) {
                // Đã thử đủ 4 lần thất bại -> Đưa vào DEAD LETTER QUEUE
                emailLog.setStatus(EmailStatus.DEAD_LETTER);
                emailLog.setErrorType("MAX_RETRIES_EXCEEDED");
                emailLog.setNextRetryAt(null);
                emailLogRepository.save(emailLog);
                log.error("EmailLog #{} FAILED after {} attempts. Moved to DEAD LETTER QUEUE: {}", emailLog.getId(), currentAttempt, errorMsg);
            } else {
                // Lên lịch retry tiếp theo theo bảng thời gian (Attempt 1: 10s -> Attempt 2: 1m -> Attempt 3: 5m)
                Duration backoff = getBackoffDuration(currentAttempt);
                emailLog.setStatus(EmailStatus.RETRYING);
                emailLog.setErrorType("TRANSIENT_SEND_ERROR");
                emailLog.setNextRetryAt(now.plus(backoff));
                emailLogRepository.save(emailLog);
                log.warn("EmailLog #{} attempt #{} FAILED ({}). Next retry #{} scheduled in {} at {}",
                        emailLog.getId(), currentAttempt, errorMsg, currentAttempt + 1, formatDuration(backoff), emailLog.getNextRetryAt());
            }
        }
    }

    /**
     * Quy tắc Backoff thời gian retry:
     * - Sau Attempt 1 fail (currentAttempt = 1) -> Chờ 10 giây -> Attempt 2
     * - Sau Attempt 2 fail (currentAttempt = 2) -> Chờ 1 phút (60s) -> Attempt 3
     * - Sau Attempt 3 fail (currentAttempt = 3) -> Chờ 5 phút (300s) -> Attempt 4
     * - Sau Attempt 4 fail (currentAttempt = 4) -> Dead Letter Queue
     */
    private Duration getBackoffDuration(int attemptCount) {
        return switch (attemptCount) {
            case 1 -> Duration.ofSeconds(10);
            case 2 -> Duration.ofMinutes(1);
            case 3 -> Duration.ofMinutes(5);
            default -> Duration.ZERO;
        };
    }

    private String formatDuration(Duration duration) {
        if (duration.toMinutes() > 0) {
            return duration.toMinutes() + " minute(s)";
        }
        return duration.toSeconds() + " second(s)";
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    @Override
    @Transactional
    public EmailLogResponse retryEmail(Long emailLogId) {
        EmailLogEntity emailLog = emailLogRepository.findById(emailLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Email log with id " + emailLogId + " not found."));

        emailLog.setStatus(EmailStatus.PENDING);
        emailLog.setAttemptCount(0);
        emailLog.setNextRetryAt(LocalDateTime.now());
        emailLog.setLastErrorMessage(null);
        emailLog.setErrorType(null);
        EmailLogEntity saved = emailLogRepository.save(emailLog);

        log.info("Manual retry triggered for EmailLog #{}. Attempt count reset to 0.", emailLogId);
        dispatchEmailAsync(saved.getId());
        return emailLogMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public EmailLogResponse resendEmailWithNewRecipient(Long emailLogId, String newRecipientEmail, String notes) {
        EmailLogEntity emailLog = emailLogRepository.findById(emailLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Email log with id " + emailLogId + " not found."));

        if (newRecipientEmail == null || newRecipientEmail.isBlank() || !isValidEmail(newRecipientEmail)) {
            throw new BadRequestException("Địa chỉ email mới không hợp lệ: '" + newRecipientEmail + "'");
        }

        emailLog.setRecipientEmail(newRecipientEmail.trim());
        emailLog.setStatus(EmailStatus.PENDING);
        emailLog.setAttemptCount(0);
        emailLog.setNextRetryAt(LocalDateTime.now());
        emailLog.setLastErrorMessage(null);
        emailLog.setErrorType(null);
        if (notes != null && !notes.isBlank()) {
            emailLog.setMetadataInfo((emailLog.getMetadataInfo() != null ? emailLog.getMetadataInfo() + " | " : "") + "Resent to: " + newRecipientEmail + " (" + notes + ")");
        }

        EmailLogEntity saved = emailLogRepository.save(emailLog);
        log.info("EmailLog #{} recipient updated to '{}' and scheduled for immediate resend.", emailLogId, newRecipientEmail);
        dispatchEmailAsync(saved.getId());
        return emailLogMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmailLogResponse getEmailLogById(Long id) {
        EmailLogEntity emailLog = emailLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Email log with id " + id + " not found."));
        return emailLogMapper.toResponse(emailLog);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmailLogResponse> getEmailLogs(Map<String, Object> params, List<String> sortByParams) {
        return queryEmailLogs(params, sortByParams, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmailLogResponse> getDeadLetterQueue(Map<String, Object> params, List<String> sortByParams) {
        return queryEmailLogs(params, sortByParams, EmailStatus.DEAD_LETTER);
    }

    @Override
    @Transactional(readOnly = true)
    public long countDeadLetterEmails() {
        return emailLogRepository.countByStatus(EmailStatus.DEAD_LETTER);
    }

    private PageResponse<EmailLogResponse> queryEmailLogs(Map<String, Object> params, List<String> sortByParams, EmailStatus forcedStatus) {
        EmailStatus status = forcedStatus;
        if (status == null && params != null && params.containsKey("status") && params.get("status") != null) {
            String s = params.get("status").toString().trim();
            if (!s.isBlank()) {
                try {
                    status = EmailStatus.valueOf(s.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        String recipientEmail = readParam(params, "recipientEmail");
        String keyword = readParam(params, "keyword");
        LocalDate fromDate = parseLocalDate(readParam(params, "createdFrom"));
        LocalDate toDate = parseLocalDate(readParam(params, "createdTo"));

        LocalDateTime createdFrom = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime createdTo = toDate != null ? toDate.atTime(23, 59, 59, 999_999_999) : null;

        Pageable pageable = createPageable(params, sortByParams);
        Specification<EmailLogEntity> spec = EmailLogSpecification.withFilters(status, recipientEmail, keyword, createdFrom, createdTo);
        Page<EmailLogEntity> page = emailLogRepository.findAll(spec, pageable);

        return PageResponse.of(page.map(emailLogMapper::toResponse));
    }

    private Pageable createPageable(Map<String, Object> params, List<String> sortByParams) {
        int page = 0;
        int size = 10;
        if (params != null) {
            if (params.get("page") != null) {
                try { page = Math.max(0, Integer.parseInt(params.get("page").toString().trim())); } catch (Exception ignored) {}
            }
            if (params.get("size") != null) {
                try { size = Math.max(1, Integer.parseInt(params.get("size").toString().trim())); } catch (Exception ignored) {}
            }
        }
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        return PageRequest.of(page, size, sort);
    }

    private String readParam(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key) || params.get(key) == null) {
            return null;
        }
        String val = params.get(key).toString().trim();
        return val.isEmpty() ? null : val;
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildInvoiceEmailHtml(InvoiceEntity invoice) {
        StringBuilder itemsHtml = new StringBuilder();
        int index = 1;
        if (invoice.getItems() != null) {
            for (InvoiceItemEntity item : invoice.getItems()) {
                String bg = (index % 2 == 0) ? "#f8fafc" : "#ffffff";
                itemsHtml.append(String.format(
                        """
                        <tr style="background-color: %s;">
                            <td style="padding: 10px 12px; border-bottom: 1px solid #e2e8f0; text-align: center; color: #64748b;">%d</td>
                            <td style="padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-weight: 500; color: #1e293b;">%s</td>
                            <td style="padding: 10px 12px; border-bottom: 1px solid #e2e8f0; text-align: center; color: #334155;">%d</td>
                            <td style="padding: 10px 12px; border-bottom: 1px solid #e2e8f0; text-align: right; color: #334155;">%s</td>
                            <td style="padding: 10px 12px; border-bottom: 1px solid #e2e8f0; text-align: right; font-weight: 600; color: #0f172a;">%s</td>
                        </tr>
                        """,
                        bg,
                        index++,
                        escapeHtml(item.getProductName()),
                        item.getQuantity() != null ? item.getQuantity() : 1,
                        formatCurrency(item.getUnitPrice()),
                        formatCurrency(item.getSubtotal())
                ));
            }
        }

        String orderIdStr = (invoice.getOrder() != null && invoice.getOrder().getId() != null)
                ? String.valueOf(invoice.getOrder().getId())
                : "N/A";

        String issueDateStr = (invoice.getIssueDate() != null)
                ? invoice.getIssueDate().format(DATE_TIME_FORMATTER)
                : "";

        String paymentMethodStr = (invoice.getPaymentMethod() != null)
                ? invoice.getPaymentMethod().name()
                : "Online";

        StringBuilder discountRow = new StringBuilder();
        if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discountRow.append(String.format(
                    """
                    <tr>
                        <td style="padding: 6px 12px; color: #64748b; text-align: right;">Giảm giá:</td>
                        <td style="padding: 6px 12px; text-align: right; color: #16a34a; font-weight: 500;">-%s</td>
                    </tr>
                    """,
                    formatCurrency(invoice.getDiscountAmount())
            ));
        }

        StringBuilder taxRow = new StringBuilder();
        if (invoice.getTaxAmount() != null && invoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            taxRow.append(String.format(
                    """
                    <tr>
                        <td style="padding: 6px 12px; color: #64748b; text-align: right;">Thuế VAT:</td>
                        <td style="padding: 6px 12px; text-align: right; color: #334155; font-weight: 500;">%s</td>
                    </tr>
                    """,
                    formatCurrency(invoice.getTaxAmount())
            ));
        }

        return String.format(
                """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Hóa đơn điện tử</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #334155; -webkit-font-smoothing: antialiased;">
                    <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f1f5f9; padding: 30px 15px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="100%%" max-width="640" style="max-width: 640px; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);">
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #1e3a8a 0%%, #3b82f6 100%%); padding: 32px 30px; text-align: center; color: #ffffff;">
                                            <h1 style="margin: 0 0 8px 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px;">%s</h1>
                                            <p style="margin: 0; font-size: 14px; opacity: 0.9;">CẢM ƠN QUÝ KHÁCH ĐÃ MUA HÀNG</p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 30px;">
                                            <p style="margin: 0 0 16px 0; font-size: 16px; color: #1e293b;">
                                                Kính chào <strong>%s</strong>,
                                            </p>
                                            <p style="margin: 0 0 24px 0; font-size: 14px; line-height: 1.6; color: #475569;">
                                                Đơn hàng <strong>#%s</strong> của quý khách đã được xác nhận thanh toán thành công. Dưới đây là thông tin chi tiết hóa đơn thanh toán:
                                            </p>
                                            <table width="100%%" style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin-bottom: 24px;" cellspacing="0" cellpadding="0">
                                                <tr>
                                                    <td width="50%%" style="vertical-align: top; padding: 6px;">
                                                        <div style="font-size: 12px; color: #64748b; margin-bottom: 2px;">MÃ HÓA ĐƠN</div>
                                                        <div style="font-size: 14px; font-weight: 600; color: #0f172a;">%s</div>
                                                    </td>
                                                    <td width="50%%" style="vertical-align: top; padding: 6px;">
                                                        <div style="font-size: 12px; color: #64748b; margin-bottom: 2px;">NGÀY PHÁT HÀNH</div>
                                                        <div style="font-size: 14px; font-weight: 600; color: #0f172a;">%s</div>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td width="50%%" style="vertical-align: top; padding: 6px;">
                                                        <div style="font-size: 12px; color: #64748b; margin-bottom: 2px;">PHƯƠNG THỨC THANH TOÁN</div>
                                                        <div style="font-size: 14px; font-weight: 600; color: #0f172a;">%s</div>
                                                    </td>
                                                    <td width="50%%" style="vertical-align: top; padding: 6px;">
                                                        <div style="font-size: 12px; color: #64748b; margin-bottom: 2px;">TRẠNG THÁI</div>
                                                        <div style="font-size: 14px; font-weight: 600; color: #16a34a;">ĐÃ THANH TOÁN</div>
                                                    </td>
                                                </tr>
                                            </table>
                                            <table width="100%%" style="border-collapse: collapse; margin-bottom: 20px;" cellspacing="0" cellpadding="0">
                                                <thead>
                                                    <tr style="background-color: #f1f5f9;">
                                                        <th style="padding: 10px 12px; border-bottom: 2px solid #cbd5e1; text-align: center; font-size: 12px; font-weight: 600; color: #475569;">#</th>
                                                        <th style="padding: 10px 12px; border-bottom: 2px solid #cbd5e1; text-align: left; font-size: 12px; font-weight: 600; color: #475569;">Sản phẩm</th>
                                                        <th style="padding: 10px 12px; border-bottom: 2px solid #cbd5e1; text-align: center; font-size: 12px; font-weight: 600; color: #475569;">SL</th>
                                                        <th style="padding: 10px 12px; border-bottom: 2px solid #cbd5e1; text-align: right; font-size: 12px; font-weight: 600; color: #475569;">Đơn giá</th>
                                                        <th style="padding: 10px 12px; border-bottom: 2px solid #cbd5e1; text-align: right; font-size: 12px; font-weight: 600; color: #475569;">Thành tiền</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    %s
                                                </tbody>
                                            </table>
                                            <table width="100%%" cellspacing="0" cellpadding="0" style="margin-bottom: 24px;">
                                                <tr>
                                                    <td width="50%%"></td>
                                                    <td width="50%%">
                                                        <table width="100%%" cellspacing="0" cellpadding="0">
                                                            <tr>
                                                                <td style="padding: 6px 12px; color: #64748b; text-align: right; font-size: 13px;">Tạm tính:</td>
                                                                <td style="padding: 6px 12px; text-align: right; color: #334155; font-size: 13px; font-weight: 500;">%s</td>
                                                            </tr>
                                                            %s
                                                            %s
                                                            <tr>
                                                                <td style="padding: 12px 12px; border-top: 2px solid #e2e8f0; color: #0f172a; text-align: right; font-size: 15px; font-weight: 700;">Tổng cộng:</td>
                                                                <td style="padding: 12px 12px; border-top: 2px solid #e2e8f0; text-align: right; color: #2563eb; font-size: 18px; font-weight: 700;">%s</td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>
                                            <div style="background-color: #eff6ff; border-left: 4px solid #3b82f6; padding: 14px 16px; border-radius: 6px; margin-bottom: 24px;">
                                                <p style="margin: 0; font-size: 13px; color: #1e40af; line-height: 1.5;">
                                                    📎 <strong>File đính kèm:</strong> Bản in hóa đơn điện tử chính thức định dạng PDF (<strong>HoaDon_%s.pdf</strong>) đã được đính kèm trong thư này. Quý khách có thể tải về để lưu trữ hoặc in ấn.
                                                </p>
                                            </div>
                                            <p style="margin: 0; font-size: 13px; color: #64748b; line-height: 1.5;">
                                                Nếu quý khách có bất kỳ câu hỏi nào, vui lòng liên hệ với chúng tôi qua hotline: <strong>%s</strong> hoặc email: <strong>%s</strong>.
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px 30px; text-align: center; font-size: 12px; color: #94a3b8;">
                                            <p style="margin: 0 0 4px 0; font-weight: 600; color: #64748b;">%s</p>
                                            <p style="margin: 0 0 4px 0;">Địa chỉ: %s</p>
                                            <p style="margin: 0;">Email này được gửi tự động, vui lòng không phản hồi trực tiếp vào địa chỉ này.</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """,
                escapeHtml(storeName),
                escapeHtml(invoice.getBillingName() != null ? invoice.getBillingName() : "Quý khách"),
                orderIdStr,
                escapeHtml(invoice.getInvoiceNumber()),
                issueDateStr,
                paymentMethodStr,
                itemsHtml.toString(),
                formatCurrency(invoice.getSubtotal()),
                discountRow.toString(),
                taxRow.toString(),
                formatCurrency(invoice.getTotalAmount()),
                escapeHtml(invoice.getInvoiceNumber()),
                escapeHtml(storePhone),
                escapeHtml(storeEmail),
                escapeHtml(storeName),
                escapeHtml(storeAddress)
        );
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        return CURRENCY_FORMAT.format(amount);
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
