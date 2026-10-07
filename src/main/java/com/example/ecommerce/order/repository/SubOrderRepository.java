package com.example.ecommerce.order.repository;

import com.example.ecommerce.order.entity.SubOrderEntity;
import com.example.ecommerce.order.enums.SubOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubOrderRepository extends JpaRepository<SubOrderEntity, Long>, JpaSpecificationExecutor<SubOrderEntity> {

    List<SubOrderEntity> findByMasterOrderId(Long masterOrderId);

    List<SubOrderEntity> findByManagedById(Long managedByUserId);

    List<SubOrderEntity> findByManagedByUsername(String managedByUsername);

    Optional<SubOrderEntity> findBySubOrderCode(String subOrderCode);

    @Query("select s from SubOrderEntity s where s.id = :id")
    Optional<SubOrderEntity> findByIdForUpdate(@Param("id") Long id);

    Page<SubOrderEntity> findAll(Specification<SubOrderEntity> spec, Pageable pageable);
}
