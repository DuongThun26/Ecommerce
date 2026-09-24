package com.example.ecommerce.order.dto.response;

import com.example.ecommerce.order.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusHistoryResponse {
    private Long id;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private String actorType;
    private String actorIdentifier;
    private String reason;
    private LocalDateTime createdAt;
}
