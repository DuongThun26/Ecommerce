package com.example.ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemResponse {
    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
}
