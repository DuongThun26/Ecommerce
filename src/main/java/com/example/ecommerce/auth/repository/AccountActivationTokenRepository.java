package com.example.ecommerce.auth.repository;

import com.example.ecommerce.auth.entity.AccountActivationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationTokenEntity, Long> {

    Optional<AccountActivationTokenEntity> findByToken(String token);
}
