package com.example.ecommerce.product.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductImageResponse {
    private Long id;
    private String imageUrl;
    private boolean isPrimary;
    private Integer displayOrder;
}
