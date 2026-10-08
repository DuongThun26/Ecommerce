package com.example.ecommerce.mail.service;

import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.invoice.entity.InvoiceEntity;
import com.example.ecommerce.mail.dto.response.EmailLogResponse;
import com.example.ecommerce.mail.entity.EmailLogEntity;

import java.util.List;
import java.util.Map;

public interface EmailService {

    void sendEmail(String to, String subject, String contentHtml);

    void sendEmailWithAttachment(String to, String subject, String contentHtml, String attachmentFileName, byte[] attachmentBytes, String contentType);

    void sendInvoiceEmail(InvoiceEntity invoice, byte[] invoicePdfBytes);

    void sendRefundEmail(com.example.ecommerce.order.entity.OrderEntity order, com.example.ecommerce.payment.entity.PaymentEntity payment, java.math.BigDecimal refundAmount, String reason);

    EmailLogEntity queueEmail(String to, String subject, String contentHtml, String attachmentFileName, String attachmentStorageKey, String attachmentContentType, String metadata);

    void processEmailLog(Long emailLogId);

    EmailLogResponse retryEmail(Long emailLogId);

    EmailLogResponse resendEmailWithNewRecipient(Long emailLogId, String newRecipientEmail, String notes);

    EmailLogResponse getEmailLogById(Long id);

    PageResponse<EmailLogResponse> getEmailLogs(Map<String, Object> params, List<String> sortByParams);

    PageResponse<EmailLogResponse> getDeadLetterQueue(Map<String, Object> params, List<String> sortByParams);

    long countDeadLetterEmails();
}
