package com.example.ecommerce.specification;

import com.example.ecommerce.dto.request.ProductSearchRequest;
import com.example.ecommerce.entity.ProductEntity;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecification {

    public static Specification<ProductEntity> withFilters(ProductSearchRequest searchRequest) {
        return deletedFalse()
                .and(byKeyword(searchRequest.getKeyword()))
                .and(byStatus(searchRequest.getStatus()))
                .and(byPriceRange(searchRequest.getMinPrice(), searchRequest.getMaxPrice()));
    }

    private static Specification<ProductEntity> deletedFalse() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("deleted"), false);
    }

    private static Specification<ProductEntity> byKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + keyword.toLowerCase() + "%"
            );
        };
    }

    private static Specification<ProductEntity> byStatus(com.example.ecommerce.enums.ProductStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    private static Specification<ProductEntity> byPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null && maxPrice == null) {
                return null;
            }
            if (minPrice != null && maxPrice != null) {
                return criteriaBuilder.between(root.get("price"), minPrice, maxPrice);
            } else if (minPrice != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
            } else {
                return criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
            }
        };
    }
}
