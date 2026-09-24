package com.example.ecommerce.order.repository;

import com.example.ecommerce.order.entity.OrderStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistoryEntity, Long> {
    List<OrderStatusHistoryEntity> findAllByOrderIdOrderByCreatedAtAsc(Long orderId);
}
