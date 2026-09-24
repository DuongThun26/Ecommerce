package com.example.ecommerce.invoice.dto.response;

import com.example.ecommerce.invoice.enums.InvoiceStatus;
import com.example.ecommerce.payment.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private Long orderId;
    private Long customerId;
    private InvoiceStatus status;
    private LocalDateTime issueDate;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String billingName;
    private String billingEmail;
    private String billingPhone;
    private String billingAddress;
    private PaymentMethod paymentMethod;
    private String paymentTransactionRef;
    private String pdfStorageKey;
    private LocalDateTime pdfGeneratedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<InvoiceItemResponse> items;
}
