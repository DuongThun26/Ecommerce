package com.example.ecommerce.product.dto.response;

import com.example.ecommerce.product.enums.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponseSummary {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private ProductStatus status;
    private String primaryImageUrl;
    private List<String> imageUrls;
}
