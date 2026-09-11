package com.example.ecommerce.dto.response;

import com.example.ecommerce.enums.ProductStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private ProductStatus status;
}
