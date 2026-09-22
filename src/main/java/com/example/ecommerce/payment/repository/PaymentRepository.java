package com.example.ecommerce.payment.repository;

import com.example.ecommerce.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByTransactionRef(String transactionRef);
    Optional<PaymentEntity> findByOrderId(Long orderId);
}
