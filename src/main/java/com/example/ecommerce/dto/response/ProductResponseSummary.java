package com.example.ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponseSummary {
    private Long id;
    private String name;
    private BigDecimal price;
}
