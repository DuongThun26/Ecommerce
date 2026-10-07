package com.example.ecommerce.order.dto.response;

import com.example.ecommerce.order.enums.SubOrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubOrderResponse {
    private Long id;
    private String subOrderCode;
    private Long masterOrderId;
    private Long managedByUserId;
    private String managedByUsername;
    private BigDecimal totalAmount;
    private SubOrderStatus status;
    private LocalDateTime statusChangedAt;
    private Long approvedById;
    private String approvedByUsername;
    private LocalDateTime approvedAt;
    private String rejectedReason;
    private String returnedReason;
    private String reviewNotes;
    private Long ticketId;
    private String ticketCode;
    private List<OrderItemResponse> orderItems;
}
