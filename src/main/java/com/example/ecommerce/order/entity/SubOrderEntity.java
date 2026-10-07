package com.example.ecommerce.order.entity;

import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.common.BaseEntity;
import com.example.ecommerce.order.enums.SubOrderStatus;
import com.example.ecommerce.ticket.entity.PurchaseTicketEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sub_orders")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubOrderEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_order_code", length = 50, nullable = false)
    private String subOrderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_order_id", nullable = false)
    private OrderEntity masterOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_by_user_id")
    private UserEntity managedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private UserEntity approvedBy;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubOrderStatus status;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_reason", length = 500)
    private String rejectedReason;

    @Column(name = "returned_reason", length = 500)
    private String returnedReason;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @OneToMany(mappedBy = "subOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItemEntity> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "subOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PurchaseTicketEntity ticket;
}
