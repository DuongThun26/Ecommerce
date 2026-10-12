package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {
    List<ProductEntity> findAllByDeletedFalse();
    Optional<ProductEntity> findByIdAndDeletedFalse(Long id);
    List<ProductEntity> findByIdInAndDeletedFalse(Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductEntity p where p.id in :ids and p.deleted = false")
    List<ProductEntity> findByIdInAndDeletedFalseForUpdate(@Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductEntity p where p.id = :id and p.deleted = false")
    Optional<ProductEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.status = com.example.ecommerce.product.enums.ProductStatus.ACTIVE
          AND p.stock <= :maxStock
          AND p.deleted = false
        ORDER BY p.stock ASC, p.id ASC
    """)
    List<ProductEntity> findLowStockActiveProducts(@Param("maxStock") int maxStock);
}

