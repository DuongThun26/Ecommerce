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
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private ProductStatus status;
    private Long categoryId;
    private String categoryName;
    private String categoryCode;
    private Long managedByUserId;
    private String managedByUsername;
    private String primaryImageUrl;
    private List<String> imageUrls;
    private List<ProductImageResponse> images;
}
