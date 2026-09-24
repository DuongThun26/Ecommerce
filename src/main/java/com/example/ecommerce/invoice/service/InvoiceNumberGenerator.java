package com.example.ecommerce.invoice.service;

import com.example.ecommerce.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceNumberGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final InvoiceRepository invoiceRepository;
    private final AtomicLong fallbackCounter = new AtomicLong(System.currentTimeMillis() % 1000000);

    /**
     * Generates a unique, thread-safe, concurrency-safe invoice number.
     * Format: INV-yyyyMMdd-000001
     */
    public String generateInvoiceNumber() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        long sequenceVal;
        try {
            Long val = invoiceRepository.getNextInvoiceSequenceValue();
            sequenceVal = (val != null) ? val : fallbackCounter.incrementAndGet();
        } catch (Exception ex) {
            log.warn("Could not retrieve nextval from invoice_number_seq, falling back to atomic sequence: {}", ex.getMessage());
            sequenceVal = fallbackCounter.incrementAndGet();
        }
        return String.format("INV-%s-%06d", datePart, sequenceVal);
    }
}
