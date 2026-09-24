package com.example.ecommerce.invoice.entity;

import com.example.ecommerce.common.BaseEntity;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.invoice.enums.InvoiceStatus;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.payment.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InvoiceStatus status;

    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "billing_name", nullable = false)
    private String billingName;

    @Column(name = "billing_email", nullable = false)
    private String billingEmail;

    @Column(name = "billing_phone", nullable = false, length = 50)
    private String billingPhone;

    @Column(name = "billing_address", length = 500)
    private String billingAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_transaction_ref", length = 100)
    private String paymentTransactionRef;

    @Column(name = "pdf_storage_key", length = 500)
    private String pdfStorageKey;

    @Column(name = "pdf_generated_at")
    private LocalDateTime pdfGeneratedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceItemEntity> items = new ArrayList<>();

    public void addItem(InvoiceItemEntity item) {
        items.add(item);
        item.setInvoice(this);
    }
}
