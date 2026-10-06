package com.example.ecommerce.order.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private Long categoryId;
    private String categoryName;
    private String categoryCode;
    private Long managedByUserId;
    private String managedByUsername;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
}
