package com.example.ecommerce.service;

import com.example.ecommerce.common.exception.EmailNotResendableException;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.mail.dto.response.EmailOutboxResponse;
import com.example.ecommerce.mail.entity.EmailOutboxEntity;
import com.example.ecommerce.mail.enums.EmailOutboxStatus;
import com.example.ecommerce.mail.repository.EmailOutboxRepository;
import com.example.ecommerce.mail.service.impl.EmailOutboxServiceImpl;
import com.example.ecommerce.order.entity.OrderEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailOutboxServiceTest {

    @Mock
    private EmailOutboxRepository emailOutboxRepository;

    @InjectMocks
    private EmailOutboxServiceImpl emailOutboxService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailOutboxService, "failAfterPaid", false);
    }

    @Test
    @DisplayName("Quy tắc 1: Khi đơn hàng chuyển PAID -> ghi 1 dòng EMAIL_OUTBOX (TEMPLATE = ORDER_PAID)")
    void recordOrderPaidOutboxShouldSavePendingRecord() {
        OrderEntity order = new OrderEntity();
        order.setId(100L);
        CustomerEntity customer = CustomerEntity.builder()
                .name("Nguyễn Văn An")
                .email("an.nguyen@example.com")
                .build();
        order.setCustomer(customer);

        when(emailOutboxRepository.findByTemplateAndReferenceId("ORDER_PAID", 100L))
                .thenReturn(Optional.empty());

        emailOutboxService.recordOrderPaidOutbox(order);

        ArgumentCaptor<EmailOutboxEntity> captor = ArgumentCaptor.forClass(EmailOutboxEntity.class);
        verify(emailOutboxRepository).save(captor.capture());

        EmailOutboxEntity saved = captor.getValue();
        assertThat(saved.getTemplate()).isEqualTo("ORDER_PAID");
        assertThat(saved.getReferenceId()).isEqualTo(100L);
        assertThat(saved.getRecipient()).isEqualTo("an.nguyen@example.com");
        assertThat(saved.getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
        assertThat(saved.getAttempts()).isEqualTo(0);
        assertThat(saved.getNextAttemptAt()).isNotNull();
    }

    @Test
    @DisplayName("Kịch bản 2: Callback VNPay gửi lại -> không ghi trùng lặp outbox (chỉ 1 dòng)")
    void recordOrderPaidOutboxDuplicateShouldBeSkipped() {
        OrderEntity order = new OrderEntity();
        order.setId(100L);

        when(emailOutboxRepository.findByTemplateAndReferenceId("ORDER_PAID", 100L))
                .thenReturn(Optional.of(new EmailOutboxEntity()));

        emailOutboxService.recordOrderPaidOutbox(order);

        verify(emailOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Quy tắc 11 & Kịch bản 8: Ở profile demo cờ app.demo.fail-after-paid=true ném exception sau khi ghi outbox")
    void recordOrderPaidOutboxWithFailAfterPaidShouldThrowException() {
        ReflectionTestUtils.setField(emailOutboxService, "failAfterPaid", true);

        OrderEntity order = new OrderEntity();
        order.setId(105L);
        CustomerEntity customer = CustomerEntity.builder()
                .name("Trần Văn Demo")
                .email("demo@example.com")
                .build();
        order.setCustomer(customer);

        when(emailOutboxRepository.findByTemplateAndReferenceId("ORDER_PAID", 105L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailOutboxService.recordOrderPaidOutbox(order))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("app.demo.fail-after-paid=true");

        verify(emailOutboxRepository).save(any());
    }

    @Test
    @DisplayName("Quy tắc 10: resend email FAILED -> đặt STATUS = PENDING, ATTEMPTS = 0, NEXT_ATTEMPT_AT = hiện tại")
    void resendFailedEmailShouldResetStatusAndAttempts() {
        EmailOutboxEntity failedOutbox = EmailOutboxEntity.builder()
                .id(1L)
                .template("ORDER_PAID")
                .referenceId(100L)
                .recipient("an.nguyen@example.com")
                .status(EmailOutboxStatus.FAILED)
                .attempts(5)
                .lastError("SMTP Connection Refused")
                .build();

        when(emailOutboxRepository.findById(1L)).thenReturn(Optional.of(failedOutbox));
        when(emailOutboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmailOutboxResponse response = emailOutboxService.resendFailedEmail(1L);

        assertThat(response.getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
        assertThat(response.getAttempts()).isEqualTo(0);
        assertThat(response.getLastError()).isNull();
        assertThat(response.getNextAttemptAt()).isNotNull();
    }

    @Test
    @DisplayName("Quy tắc 10: resend email không ở trạng thái FAILED -> ném 409 EMAIL_NOT_RESENDABLE")
    void resendNonFailedEmailShouldThrowEmailNotResendableException() {
        EmailOutboxEntity pendingOutbox = EmailOutboxEntity.builder()
                .id(2L)
                .status(EmailOutboxStatus.PENDING)
                .build();

        when(emailOutboxRepository.findById(2L)).thenReturn(Optional.of(pendingOutbox));

        assertThatThrownBy(() -> emailOutboxService.resendFailedEmail(2L))
                .isInstanceOf(EmailNotResendableException.class)
                .hasMessageContaining("PENDING");
    }
}
