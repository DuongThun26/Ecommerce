package com.example.ecommerce.order.specification;

import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class  OrderSpecification {

    // Ghép các điều kiện lọc đơn hàng vào một Specification duy nhất.
    public static Specification<OrderEntity> withFilters(
            Long customerId,
            OrderStatus status,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            BigDecimal minTotalAmount,
            BigDecimal maxTotalAmount,
            String keyword
    ) {
        return byCustomerId(customerId)
                .and(byStatus(status))
                .and(byCreatedAtFrom(createdFrom))
                .and(byCreatedAtTo(createdTo))
                .and(byMinTotalAmount(minTotalAmount))
                .and(byMaxTotalAmount(maxTotalAmount))
                .and(byKeyword(keyword));
    }

    // Lọc theo customer khi API yêu cầu xem đơn hàng của một khách hàng cụ thể.
    private static Specification<OrderEntity> byCustomerId(Long customerId) {
        return (root, query, criteriaBuilder) -> {
            if (customerId == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("customer").get("id"), customerId);
        };
    }

    // Lọc theo trạng thái đơn hàng nếu client truyền status.
    private static Specification<OrderEntity> byStatus(OrderStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    private static Specification<OrderEntity> byCreatedAtFrom(LocalDateTime createdFrom) {
        return (root, query, criteriaBuilder) -> {
            if (createdFrom == null) {
                return null;
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
        };
    }

    private static Specification<OrderEntity> byCreatedAtTo(LocalDateTime createdTo) {
        return (root, query, criteriaBuilder) -> {
            if (createdTo == null) {
                return null;
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
        };
    }

    private static Specification<OrderEntity> byMinTotalAmount(BigDecimal minTotalAmount) {
        return (root, query, criteriaBuilder) -> {
            if (minTotalAmount == null) {
                return null;
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("totalAmount"), minTotalAmount);
        };
    }

    private static Specification<OrderEntity> byMaxTotalAmount(BigDecimal maxTotalAmount) {
        return (root, query, criteriaBuilder) -> {
            if (maxTotalAmount == null) {
                return null;
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("totalAmount"), maxTotalAmount);
        };
    }

    private static Specification<OrderEntity> byKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("customer").get("name")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("customer").get("email")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("customer").get("phone")), pattern));
            return criteriaBuilder.or(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
