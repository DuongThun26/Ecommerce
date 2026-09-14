package com.example.ecommerce.specification;

import com.example.ecommerce.entity.OrderEntity;
import com.example.ecommerce.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

public class  OrderSpecification {

    // Ghép các điều kiện lọc đơn hàng vào một Specification duy nhất.
    public static Specification<OrderEntity> withFilters(Long customerId, OrderStatus status) {
        return byCustomerId(customerId)
                .and(byStatus(status));
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
}
