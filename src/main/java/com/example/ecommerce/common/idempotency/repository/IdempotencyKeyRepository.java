package com.example.ecommerce.common.idempotency.repository;

import com.example.ecommerce.common.idempotency.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, Long> {

    Optional<IdempotencyKeyEntity> findByIdempotencyKey(String idempotencyKey);
}
