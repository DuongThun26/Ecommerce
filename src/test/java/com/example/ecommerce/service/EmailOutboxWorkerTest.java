package com.example.ecommerce.service;

import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.mail.entity.EmailOutboxEntity;
import com.example.ecommerce.mail.enums.EmailOutboxStatus;
import com.example.ecommerce.mail.repository.EmailOutboxRepository;
import com.example.ecommerce.mail.scheduler.EmailOutboxWorker;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.repository.OrderItemRepository;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.product.entity.ProductEntity;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailOutboxWorkerTest {

    @Mock
    private EmailOutboxRepository emailOutboxRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private TemplateEngine templateEngine;
    @Mock
    private Environment environment;

    @InjectMocks
    private EmailOutboxWorker worker;

    @BeforeEach
    void setUp() {
        lenient().when(environment.getActiveProfiles()).thenReturn(new String[]{"default"});
    }

    @Test
    @DisplayName("Quy tắc 4: Gửi mail thành công -> STATUS = SENT và ghi SENT_AT")
    void processSingleOutboxSuccessShouldMarkSent() {
        EmailOutboxEntity outbox = EmailOutboxEntity.builder()
                .id(1L)
                .template("ORDER_PAID")
                .referenceId(100L)
                .recipient("an.nguyen@example.com")
                .status(EmailOutboxStatus.PENDING)
                .attempts(0)
                .build();

        OrderEntity order = new OrderEntity();
        order.setId(100L);
        order.setTotalAmount(BigDecimal.valueOf(45980000));
        order.setCustomer(CustomerEntity.builder().name("Nguyễn Văn An").build());

        OrderItemEntity item = OrderItemEntity.builder()
                .product(ProductEntity.builder().name("iPhone 16 128GB").build())
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(22990000))
                .subTotal(BigDecimal.valueOf(45980000))
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(item));
        when(templateEngine.process(eq("mail/order-paid"), any(IContext.class))).thenReturn("<html>mock</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        worker.processSingleOutbox(outbox);

        assertThat(outbox.getStatus()).isEqualTo(EmailOutboxStatus.SENT);
        assertThat(outbox.getSentAt()).isNotNull();
        assertThat(outbox.getLastError()).isNull();
        verify(emailOutboxRepository).save(outbox);
    }

    @Test
    @DisplayName("Quy tắc 5: Gửi lỗi lần 1 -> ATTEMPTS = 1, ghi LAST_ERROR, NEXT_ATTEMPT_AT = hiện tại + backoff")
    void processSingleOutboxErrorShouldRetryWithExponentialBackoff() {
        EmailOutboxEntity outbox = EmailOutboxEntity.builder()
                .id(1L)
                .template("ORDER_PAID")
                .referenceId(100L)
                .recipient("an.nguyen@example.com")
                .status(EmailOutboxStatus.PENDING)
                .attempts(0)
                .build();

        when(orderRepository.findById(100L)).thenThrow(new MailSendException("Mail server down"));

        worker.processSingleOutbox(outbox);

        assertThat(outbox.getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
        assertThat(outbox.getAttempts()).isEqualTo(1);
        assertThat(outbox.getLastError()).contains("Mail server down");
        assertThat(outbox.getNextAttemptAt()).isAfter(LocalDateTime.now().plusSeconds(50));
        verify(emailOutboxRepository).save(outbox);
    }

    @Test
    @DisplayName("Quy tắc 6: Nếu ATTEMPTS = 5 -> đặt STATUS = FAILED và KHÔNG gửi tiếp")
    void processSingleOutboxReachMaxAttemptsShouldMarkFailed() {
        EmailOutboxEntity outbox = EmailOutboxEntity.builder()
                .id(1L)
                .template("ORDER_PAID")
                .referenceId(100L)
                .recipient("an.nguyen@example.com")
                .status(EmailOutboxStatus.PENDING)
                .attempts(4) // đã thử 4 lần, lần này là lần 5
                .build();

        when(orderRepository.findById(100L)).thenThrow(new MailSendException("Mail server permanently down"));

        worker.processSingleOutbox(outbox);

        assertThat(outbox.getStatus()).isEqualTo(EmailOutboxStatus.FAILED);
        assertThat(outbox.getAttempts()).isEqualTo(5);
        verify(emailOutboxRepository).save(outbox);
    }

    @Test
    @DisplayName("Quy tắc 7: Tính toán backoff-base: 60s mặc định, 5s ở demo profile")
    void testBackoffCalculation() {
        // Mặc định (60s)
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        assertThat(worker.calculateBackoffSeconds(1)).isEqualTo(60L);  // 60 * 2^0
        assertThat(worker.calculateBackoffSeconds(2)).isEqualTo(120L); // 60 * 2^1
        assertThat(worker.calculateBackoffSeconds(3)).isEqualTo(240L); // 60 * 2^2
        assertThat(worker.calculateBackoffSeconds(4)).isEqualTo(480L); // 60 * 2^3

        // Profile demo (5s)
        when(environment.getActiveProfiles()).thenReturn(new String[]{"demo"});
        assertThat(worker.calculateBackoffSeconds(1)).isEqualTo(5L);   // 5 * 2^0
        assertThat(worker.calculateBackoffSeconds(2)).isEqualTo(10L);  // 5 * 2^1
        assertThat(worker.calculateBackoffSeconds(3)).isEqualTo(20L);  // 5 * 2^2
        assertThat(worker.calculateBackoffSeconds(4)).isEqualTo(40L);  // 5 * 2^3
    }
}
