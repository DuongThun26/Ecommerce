package com.example.ecommerce.order.repository;

import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long>, JpaSpecificationExecutor<OrderEntity> {
    boolean existsByCustomerId(Long customerId);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    List<OrderEntity> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime dateTime);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    List<OrderEntity> findByStatusAndPaymentExpiresAtBefore(OrderStatus status, LocalDateTime dateTime);

    @EntityGraph(attributePaths = {"customer"})
    @Query("select o from OrderEntity o where o.id = :id")
    Optional<OrderEntity> findByIdWithCustomer(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.id = :id")
    Optional<OrderEntity> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.customer.id = :customerId and o.status in :statuses order by o.createdAt desc")
    List<OrderEntity> findByCustomerIdAndStatusInForUpdate(@Param("customerId") Long customerId, @Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderEntity o WHERE o.paidAt >= :startOfDay AND o.paidAt < :endOfDay AND o.status IN :statuses")
    java.math.BigDecimal calculateRevenueBetween(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("statuses") java.util.Collection<OrderStatus> statuses);

    @Query("SELECT o.status, COUNT(o) FROM OrderEntity o GROUP BY o.status")
    List<Object[]> countOrdersGroupByStatus();
}

