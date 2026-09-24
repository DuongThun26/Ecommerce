package com.example.ecommerce.service;

import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.invoice.entity.InvoiceEntity;
import com.example.ecommerce.invoice.entity.InvoiceItemEntity;
import com.example.ecommerce.invoice.enums.InvoiceStatus;
import com.example.ecommerce.invoice.service.impl.OpenPdfInvoiceGeneratorImpl;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.payment.enums.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class InvoicePdfGeneratorTest {

    private OpenPdfInvoiceGeneratorImpl pdfGenerator;
    private InvoiceEntity invoice;

    @BeforeEach
    void setUp() {
        pdfGenerator = new OpenPdfInvoiceGeneratorImpl();
        ReflectionTestUtils.setField(pdfGenerator, "storeName", "E-COMMERCE STORE");
        ReflectionTestUtils.setField(pdfGenerator, "storeAddress", "123 Le Loi, District 1, HCMC");
        ReflectionTestUtils.setField(pdfGenerator, "storePhone", "+84 28 1234 5678");
        ReflectionTestUtils.setField(pdfGenerator, "storeEmail", "support@ecommerce.local");

        CustomerEntity customer = CustomerEntity.builder()
                .id(1L)
                .name("Nguyễn Văn A")
                .email("vana@example.com")
                .phone("0901234567")
                .build();

        OrderEntity order = OrderEntity.builder()
                .id(101L)
                .customer(customer)
                .totalAmount(BigDecimal.valueOf(25000000))
                .build();

        invoice = InvoiceEntity.builder()
                .id(1L)
                .invoiceNumber("INV-20260924-000001")
                .order(order)
                .customer(customer)
                .status(InvoiceStatus.ISSUED)
                .issueDate(LocalDateTime.now())
                .subtotal(BigDecimal.valueOf(25000000))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(25000000))
                .billingName("Nguyễn Văn A")
                .billingEmail("vana@example.com")
                .billingPhone("0901234567")
                .billingAddress("456 Nguyen Hue, Q1, TP.HCM")
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentTransactionRef("VNP12345678")
                .items(new ArrayList<>())
                .build();

        InvoiceItemEntity item1 = InvoiceItemEntity.builder()
                .productId(10L)
                .productName("Tai nghe Bluetooth Sony WH-1000XM5")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(8000000))
                .subtotal(BigDecimal.valueOf(8000000))
                .build();

        InvoiceItemEntity item2 = InvoiceItemEntity.builder()
                .productId(11L)
                .productName("Điện thoại thông minh Samsung Galaxy S24")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(17000000))
                .subtotal(BigDecimal.valueOf(17000000))
                .build();

        invoice.addItem(item1);
        invoice.addItem(item2);
    }

    @Test
    @DisplayName("generateInvoicePdf should produce non-empty valid PDF byte array")
    void generateInvoicePdf_ValidInvoice_ReturnsNonEmptyBytes() {
        byte[] pdfBytes = pdfGenerator.generateInvoicePdf(invoice);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(100);

        // Standard PDF magic bytes %PDF-
        String header = new String(pdfBytes, 0, 5);
        assertThat(header).isEqualTo("%PDF-");
    }
}
