package com.example.ecommerce.order.repository;

import com.example.ecommerce.order.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ecommerce.dashboard.dto.TopProductProjection;
import com.example.ecommerce.order.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
    long countByProductId(Long productId);
    java.util.List<OrderItemEntity> findByOrderId(Long orderId);

    @Query("""
        SELECT oi.product.id AS productId, oi.product.name AS name, SUM(oi.quantity) AS quantitySold
        FROM OrderItemEntity oi
        JOIN oi.order o
        WHERE o.paidAt >= :startOfDay AND o.paidAt < :endOfDay
          AND o.status IN :statuses
        GROUP BY oi.product.id, oi.product.name
        ORDER BY SUM(oi.quantity) DESC, oi.product.id ASC
    """)
    List<TopProductProjection> findTopProductsBetween(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("statuses") Collection<OrderStatus> statuses,
            Pageable pageable);
}
