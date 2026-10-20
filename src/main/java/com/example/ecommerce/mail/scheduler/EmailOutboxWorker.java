package com.example.ecommerce.mail.scheduler;

import com.example.ecommerce.mail.dto.OrderItemMailDto;
import com.example.ecommerce.mail.entity.EmailOutboxEntity;
import com.example.ecommerce.mail.enums.EmailOutboxStatus;
import com.example.ecommerce.mail.repository.EmailOutboxRepository;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.repository.OrderItemRepository;
import com.example.ecommerce.order.repository.OrderRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailOutboxWorker {

    private final EmailOutboxRepository emailOutboxRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final Environment environment;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${app.email.outbox.backoff-base-seconds:60}")
    private Long configBackoffBaseSeconds;

    /**
     * Quy tắc 3: Worker PHẢI chạy mỗi 10 giây.
     * Mỗi lần chạy, worker lấy tối đa 50 dòng PENDING có NEXT_ATTEMPT_AT <= thời điểm hiện tại, theo ID tăng dần.
     */
    @Scheduled(fixedDelay = 10000)
    public void processOutboxQueue() {
        LocalDateTime now = LocalDateTime.now();
        List<EmailOutboxEntity> pendingEmails = emailOutboxRepository
                .findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(EmailOutboxStatus.PENDING, now);

        if (pendingEmails.isEmpty()) {
            return;
        }

        log.info("EmailOutboxWorker: Found {} pending emails to process.", pendingEmails.size());

        for (EmailOutboxEntity outbox : pendingEmails) {
            processSingleOutbox(outbox);
        }
    }

    public void processSingleOutbox(EmailOutboxEntity outbox) {
        try {
            if ("ORDER_PAID".equalsIgnoreCase(outbox.getTemplate())) {
                sendOrderPaidEmail(outbox);
            } else {
                log.warn("Unknown email template: {}", outbox.getTemplate());
            }

            // Quy tắc 4: Nếu gửi thành công, worker đặt STATUS = SENT và ghi SENT_AT.
            outbox.setStatus(EmailOutboxStatus.SENT);
            outbox.setSentAt(LocalDateTime.now());
            outbox.setLastError(null);
            emailOutboxRepository.save(outbox);
            log.info("Email outbox #{} sent successfully to {}.", outbox.getId(), outbox.getRecipient());
        } catch (Exception ex) {
            // Quy tắc 5 & 6: Nếu gửi lỗi, worker tăng ATTEMPTS thêm 1 và ghi LAST_ERROR.
            int nextAttempts = outbox.getAttempts() + 1;
            outbox.setAttempts(nextAttempts);
            outbox.setLastError(truncate(ex.getMessage(), 1000));

            if (nextAttempts >= 5) {
                // Quy tắc 6: Nếu ATTEMPTS = 5, worker đặt STATUS = FAILED và KHÔNG ĐƯỢC gửi tiếp.
                outbox.setStatus(EmailOutboxStatus.FAILED);
                log.error("Email outbox #{} reached maximum 5 attempts. Marked as FAILED.", outbox.getId(), ex);
            } else {
                // Quy tắc 5: Đặt NEXT_ATTEMPT_AT = hiện tại + backoff-base * 2^(ATTEMPTS - 1).
                long backoffSeconds = calculateBackoffSeconds(nextAttempts);
                outbox.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSeconds));
                log.warn("Email outbox #{} failed (attempt {}/5). Next attempt in {}s. Error: {}",
                        outbox.getId(), nextAttempts, backoffSeconds, ex.getMessage());
            }

            emailOutboxRepository.save(outbox);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void sendOrderPaidEmail(EmailOutboxEntity outbox) throws Exception {
        Long orderId = outbox.getReferenceId();
        OrderEntity order = orderRepository.findByIdWithCustomer(orderId)
                .orElseGet(() -> orderRepository.findById(orderId).orElse(null));
        if (order == null) {
            throw new IllegalStateException("Đơn hàng #" + orderId + " không tồn tại để gửi email.");
        }

        List<OrderItemEntity> items = orderItemRepository.findByOrderId(orderId);

        String customerName = "Quý khách";
        if (order.getCustomer() != null && order.getCustomer().getName() != null && !order.getCustomer().getName().isBlank()) {
            customerName = order.getCustomer().getName();
        }

        LocalDateTime paidTime = order.getUpdatedAt() != null ? order.getUpdatedAt() : LocalDateTime.now();
        String paidAtFormatted = paidTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(VIETNAM_ZONE)
                .format(TIME_FORMATTER);

        List<OrderItemMailDto> mailItems = new ArrayList<>();
        for (OrderItemEntity item : items) {
            String prodName = item.getProduct() != null ? item.getProduct().getName() : "Sản phẩm";
            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal subtotal = item.getSubTotal() != null ? item.getSubTotal() : unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            mailItems.add(OrderItemMailDto.builder()
                    .productName(prodName)
                    .quantity(item.getQuantity())
                    .unitPriceFormatted(formatVnd(unitPrice))
                    .subtotalFormatted(formatVnd(subtotal))
                    .build());
        }

        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        String totalFormatted = formatVnd(total);

        // Quy tắc 8: Mail PHẢI dùng template Thymeleaf HTML.
        Context context = new Context();
        context.setVariable("orderId", order.getId());
        context.setVariable("customerName", customerName);
        context.setVariable("paidAt", paidAtFormatted);
        context.setVariable("items", mailItems);
        context.setVariable("totalAmountFormatted", totalFormatted);

        String html = templateEngine.process("mail/order-paid", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setTo(outbox.getRecipient());
        helper.setSubject("[Shop] Xác nhận thanh toán đơn hàng #" + order.getId());
        helper.setText(html, true);
        if (mailFrom != null && !mailFrom.isBlank()) {
            helper.setFrom(mailFrom);
        }

        mailSender.send(message);
    }

    /**
     * Quy tắc 7: backoff-base = 1 phút (60s). Ở profile demo, backoff-base = 5 giây.
     * NEXT_ATTEMPT_AT = hiện tại + backoff-base * 2^(ATTEMPTS - 1).
     */
    public long calculateBackoffSeconds(int attempts) {
        long base = getBackoffBaseSeconds();
        int exponent = Math.max(0, attempts - 1);
        return base * (1L << exponent);
    }

    public long getBackoffBaseSeconds() {
        if (Arrays.asList(environment.getActiveProfiles()).contains("demo")) {
            return 5L;
        }
        if (configBackoffBaseSeconds != null) {
            return configBackoffBaseSeconds;
        }
        return 60L;
    }

    public static String formatVnd(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(amount) + " ₫";
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength);
    }
}
