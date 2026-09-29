package com.example.ecommerce.mail.service;

import com.example.ecommerce.invoice.entity.InvoiceEntity;

public interface EmailService {

    void sendEmail(String to, String subject, String contentHtml);

    void sendEmailWithAttachment(String to, String subject, String contentHtml, String attachmentFileName, byte[] attachmentBytes, String contentType);

    void sendInvoiceEmail(InvoiceEntity invoice, byte[] invoicePdfBytes);
}
