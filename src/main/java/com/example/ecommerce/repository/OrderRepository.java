package com.example.ecommerce.repository;

import com.example.ecommerce.entity.OrderEntity;
import com.example.ecommerce.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long>, JpaSpecificationExecutor<OrderEntity> {
    boolean existsByCustomerId(Long customerId);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    List<OrderEntity> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime dateTime);
}
