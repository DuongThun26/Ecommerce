package com.example.ecommerce.repository;

import com.example.ecommerce.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
    long countByProductId(Long productId);
}
