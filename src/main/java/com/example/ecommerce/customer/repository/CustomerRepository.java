package com.example.ecommerce.customer.repository;

import com.example.ecommerce.customer.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    List<CustomerEntity> findAllByDeletedFalse();
    Optional<CustomerEntity> findByIdAndDeletedFalse(Long id);
}
