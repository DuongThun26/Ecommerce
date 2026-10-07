package com.example.ecommerce.order.entity;

import com.example.ecommerce.common.BaseEntity;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.payment.entity.PaymentEntity;
import com.example.ecommerce.ticket.entity.PurchaseTicketEntity;
import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "master_orders")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", length = 50)
    private String orderCode;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "status_changed_at", nullable = false)
    private LocalDateTime statusChangedAt;

    @Column(name = "payment_expires_at")
    private LocalDateTime paymentExpiresAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private UserEntity approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_reason", length = 500)
    private String rejectedReason;

    @Column(name = "returned_reason", length = 500)
    private String returnedReason;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItemEntity> orderItems = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "masterOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SubOrderEntity> subOrders = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PaymentEntity> payments = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PurchaseTicketEntity> tickets = new java.util.ArrayList<>();
}
