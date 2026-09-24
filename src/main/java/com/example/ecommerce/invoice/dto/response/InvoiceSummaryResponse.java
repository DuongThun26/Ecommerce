package com.example.ecommerce.invoice.dto.response;

import com.example.ecommerce.invoice.enums.InvoiceStatus;
import com.example.ecommerce.payment.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceSummaryResponse {
    private Long id;
    private String invoiceNumber;
    private Long orderId;
    private Long customerId;
    private String billingName;
    private InvoiceStatus status;
    private LocalDateTime issueDate;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private LocalDateTime createdAt;
}
