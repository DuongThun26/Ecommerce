package com.example.ecommerce.invoice.service;

import com.example.ecommerce.invoice.entity.InvoiceEntity;

public interface InvoicePdfGenerator {

    byte[] generateInvoicePdf(InvoiceEntity invoice);
}
