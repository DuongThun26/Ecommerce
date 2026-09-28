package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.entity.ProductImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Long> {
    List<ProductImageEntity> findByProductIdOrderByDisplayOrderAscCreatedAtAsc(Long productId);
    void deleteByProductId(Long productId);
}
