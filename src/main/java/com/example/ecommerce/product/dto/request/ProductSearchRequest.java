package com.example.ecommerce.product.dto.request;

import com.example.ecommerce.product.enums.ProductStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductSearchRequest {
    private String keyword;
    private ProductStatus status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private int page = 0;
    private int size = 20;
    private String sort = "id,desc";
}
