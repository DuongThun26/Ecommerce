package com.example.ecommerce.service;

import com.example.ecommerce.invoice.service.PdfStorageService;
import com.example.ecommerce.mail.entity.EmailLogEntity;
import com.example.ecommerce.mail.enums.EmailStatus;
import com.example.ecommerce.mail.mapper.EmailLogMapper;
import com.example.ecommerce.mail.repository.EmailLogRepository;
import com.example.ecommerce.mail.service.impl.EmailServiceImpl;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceRetryTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailLogRepository emailLogRepository;

    @Mock
    private EmailLogMapper emailLogMapper;

    @Mock
    private PdfStorageService pdfStorageService;

    @InjectMocks
    private EmailServiceImpl emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    }

    @Test
    @DisplayName("Invalid email recipient should move immediately to DEAD_LETTER without retries")
    void testInvalidEmailAddress_movesToDeadLetterDirectly() {
        EmailLogEntity emailLog = EmailLogEntity.builder()
                .id(1L)
                .recipientEmail("invalid-email-address")
                .subject("Test Subject")
                .contentHtml("<p>Hello</p>")
                .status(EmailStatus.PENDING)
                .attemptCount(0)
                .maxAttempts(4)
                .build();

        when(emailLogRepository.findById(1L)).thenReturn(Optional.of(emailLog));
        when(emailLogRepository.save(any(EmailLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailService.processEmailLog(1L);

        assertThat(emailLog.getStatus()).isEqualTo(EmailStatus.DEAD_LETTER);
        assertThat(emailLog.getErrorType()).isEqualTo("INVALID_RECIPIENT_EMAIL");
        assertThat(emailLog.getNextRetryAt()).isNull();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Attempt 1 failure should schedule retry in 10 seconds")
    void testAttempt1Failure_schedules10SecondsBackoff() {
        EmailLogEntity emailLog = EmailLogEntity.builder()
                .id(2L)
                .recipientEmail("customer@example.com")
                .subject("Test Invoice")
                .contentHtml("<p>Invoice Content</p>")
                .status(EmailStatus.PENDING)
                .attemptCount(0)
                .maxAttempts(4)
                .build();

        when(emailLogRepository.findById(2L)).thenReturn(Optional.of(emailLog));
        when(emailLogRepository.save(any(EmailLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP Connection Timeout")).when(mailSender).send(any(MimeMessage.class));

        LocalDateTime before = LocalDateTime.now();
        emailService.processEmailLog(2L);
        LocalDateTime after = LocalDateTime.now();

        assertThat(emailLog.getStatus()).isEqualTo(EmailStatus.RETRYING);
        assertThat(emailLog.getAttemptCount()).isEqualTo(1);
        assertThat(emailLog.getNextRetryAt()).isAfterOrEqualTo(before.plusSeconds(9));
        assertThat(emailLog.getNextRetryAt()).isBeforeOrEqualTo(after.plusSeconds(11));
    }

    @Test
    @DisplayName("Attempt 2 failure should schedule retry in 1 minute")
    void testAttempt2Failure_schedules1MinuteBackoff() {
        EmailLogEntity emailLog = EmailLogEntity.builder()
                .id(3L)
                .recipientEmail("customer@example.com")
                .subject("Test Invoice")
                .contentHtml("<p>Invoice Content</p>")
                .status(EmailStatus.RETRYING)
                .attemptCount(1) // was already attempted once
                .maxAttempts(4)
                .build();

        when(emailLogRepository.findById(3L)).thenReturn(Optional.of(emailLog));
        when(emailLogRepository.save(any(EmailLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP Connection Timeout")).when(mailSender).send(any(MimeMessage.class));

        LocalDateTime before = LocalDateTime.now();
        emailService.processEmailLog(3L);
        LocalDateTime after = LocalDateTime.now();

        assertThat(emailLog.getStatus()).isEqualTo(EmailStatus.RETRYING);
        assertThat(emailLog.getAttemptCount()).isEqualTo(2);
        assertThat(emailLog.getNextRetryAt()).isAfterOrEqualTo(before.plusSeconds(59));
        assertThat(emailLog.getNextRetryAt()).isBeforeOrEqualTo(after.plusSeconds(61));
    }

    @Test
    @DisplayName("Attempt 3 failure should schedule retry in 5 minutes")
    void testAttempt3Failure_schedules5MinutesBackoff() {
        EmailLogEntity emailLog = EmailLogEntity.builder()
                .id(4L)
                .recipientEmail("customer@example.com")
                .subject("Test Invoice")
                .contentHtml("<p>Invoice Content</p>")
                .status(EmailStatus.RETRYING)
                .attemptCount(2) // was already attempted twice
                .maxAttempts(4)
                .build();

        when(emailLogRepository.findById(4L)).thenReturn(Optional.of(emailLog));
        when(emailLogRepository.save(any(EmailLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP Connection Timeout")).when(mailSender).send(any(MimeMessage.class));

        LocalDateTime before = LocalDateTime.now();
        emailService.processEmailLog(4L);
        LocalDateTime after = LocalDateTime.now();

        assertThat(emailLog.getStatus()).isEqualTo(EmailStatus.RETRYING);
        assertThat(emailLog.getAttemptCount()).isEqualTo(3);
        assertThat(emailLog.getNextRetryAt()).isAfterOrEqualTo(before.plusSeconds(299));
        assertThat(emailLog.getNextRetryAt()).isBeforeOrEqualTo(after.plusSeconds(301));
    }

    @Test
    @DisplayName("Attempt 4 failure should move email to DEAD_LETTER queue")
    void testAttempt4Failure_movesToDeadLetterQueue() {
        EmailLogEntity emailLog = EmailLogEntity.builder()
                .id(5L)
                .recipientEmail("customer@example.com")
                .subject("Test Invoice")
                .contentHtml("<p>Invoice Content</p>")
                .status(EmailStatus.RETRYING)
                .attemptCount(3) // 3 attempts done, this will be the 4th
                .maxAttempts(4)
                .build();

        when(emailLogRepository.findById(5L)).thenReturn(Optional.of(emailLog));
        when(emailLogRepository.save(any(EmailLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP Final Fail")).when(mailSender).send(any(MimeMessage.class));

        emailService.processEmailLog(5L);

        assertThat(emailLog.getStatus()).isEqualTo(EmailStatus.DEAD_LETTER);
        assertThat(emailLog.getAttemptCount()).isEqualTo(4);
        assertThat(emailLog.getErrorType()).isEqualTo("MAX_RETRIES_EXCEEDED");
        assertThat(emailLog.getNextRetryAt()).isNull();
    }
}
