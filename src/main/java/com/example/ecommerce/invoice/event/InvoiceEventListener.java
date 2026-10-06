package com.example.ecommerce.invoice.event;

import com.example.ecommerce.invoice.entity.InvoiceEntity;
import com.example.ecommerce.invoice.repository.InvoiceRepository;
import com.example.ecommerce.invoice.service.InvoicePdfGenerator;
import com.example.ecommerce.invoice.service.InvoiceService;
import com.example.ecommerce.invoice.service.PdfStorageService;
import com.example.ecommerce.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventListener {

    private final InvoiceService invoiceService;
    private final EmailService emailService;
    private final InvoiceRepository invoiceRepository;
    private final PdfStorageService pdfStorageService;
    private final InvoicePdfGenerator invoicePdfGenerator;

    /**
     * Lắng nghe sự kiện phát hành hóa đơn (InvoiceIssuedEvent).
     * Chạy bất đồng bộ (@Async) sau khi transaction cơ sở dữ liệu đã commit thành công (AFTER_COMMIT),
     * giúp đảm bảo dữ liệu hóa đơn đã lưu hoàn tất trước khi tạo PDF và gửi email cho người mua.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleInvoiceIssued(InvoiceIssuedEvent event) {
        log.info("Received InvoiceIssuedEvent for invoiceId: {}", event.invoiceId());
        
        // 1. Tạo file PDF hóa đơn và lưu trữ vào storage
        try {
            invoiceService.generateAndStorePdf(event.invoiceId());
        } catch (Exception ex) {
            log.error("Failed to automatically generate and store PDF for invoiceId: {}", event.invoiceId(), ex);
        }

        // 2. Lấy dữ liệu PDF (từ storage hoặc tạo trực tiếp) và gửi email đính kèm cho người mua
        try {
            InvoiceEntity invoice = invoiceRepository.findByIdWithItems(event.invoiceId()).orElse(null);
            if (invoice != null && invoice.getBillingEmail() != null && !invoice.getBillingEmail().isBlank()) {
                byte[] pdfBytes = null;
                if (invoice.getPdfStorageKey() != null && pdfStorageService.exists(invoice.getPdfStorageKey())) {
                    pdfBytes = pdfStorageService.load(invoice.getPdfStorageKey());
                } else {
                    pdfBytes = invoicePdfGenerator.generateInvoicePdf(invoice);
                }

                if (pdfBytes != null && pdfBytes.length > 0) {
                    emailService.sendInvoiceEmail(invoice, pdfBytes);
                    log.info("Sent invoice email with attachment for invoice #{} to {}", invoice.getInvoiceNumber(), invoice.getBillingEmail());
                }
            } else {
                log.warn("Invoice with id {} not found or missing billing email, skipping email notification.", event.invoiceId());
            }
        } catch (Exception ex) {
            log.error("Failed to send invoice email for invoiceId: {}", event.invoiceId(), ex);
        }
    }
}

