package com.example.ecommerce.order.dto.response;

import com.example.ecommerce.order.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderCode;
    private Long customerId;
    private String customerName;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime statusChangedAt;
    private LocalDateTime paymentExpiresAt;
    private String cancelReason;
    private Long approvedById;
    private String approvedByUsername;
    private LocalDateTime approvedAt;
    private String rejectedReason;
    private String returnedReason;
    private String reviewNotes;
    private Long ticketId;
    private String ticketCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> orderItems;
    private List<SubOrderResponse> subOrders;
    private Boolean canMakeDecision;
    private Integer approvedSubOrdersCount;
    private Integer rejectedSubOrdersCount;
    private Integer pendingSubOrdersCount;
    private Integer returnedSubOrdersCount;
}
