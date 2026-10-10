package com.example.ecommerce.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderItemsResponse {
    private Long orderId;
    private String status;
    private List<OrderItemDetailResponse> items;
    private BigDecimal totalAmount;

    public Long getId() {
        return orderId;
    }
}
