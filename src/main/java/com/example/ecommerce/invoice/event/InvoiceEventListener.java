package com.example.ecommerce.invoice.event;

import com.example.ecommerce.invoice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventListener {

    private final InvoiceService invoiceService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleInvoiceIssued(InvoiceIssuedEvent event) {
        log.info("Received InvoiceIssuedEvent for invoiceId: {}", event.invoiceId());
        try {
            invoiceService.generateAndStorePdf(event.invoiceId());
        } catch (Exception ex) {
            log.error("Failed to automatically generate and store PDF for invoiceId: {}", event.invoiceId(), ex);
        }
    }
}
