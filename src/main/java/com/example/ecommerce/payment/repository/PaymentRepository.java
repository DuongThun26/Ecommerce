package com.example.ecommerce.payment.repository;

import com.example.ecommerce.payment.entity.PaymentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByTransactionRef(String transactionRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p where p.transactionRef = :transactionRef")
    Optional<PaymentEntity> findByTransactionRefForUpdate(@Param("transactionRef") String transactionRef);

    Optional<PaymentEntity> findByOrderId(Long orderId);
}
