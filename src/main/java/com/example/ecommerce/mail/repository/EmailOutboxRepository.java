package com.example.ecommerce.mail.repository;

import com.example.ecommerce.mail.entity.EmailOutboxEntity;
import com.example.ecommerce.mail.enums.EmailOutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutboxEntity, Long>, JpaSpecificationExecutor<EmailOutboxEntity> {

    Optional<EmailOutboxEntity> findByTemplateAndReferenceId(String template, Long referenceId);

    List<EmailOutboxEntity> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(
            EmailOutboxStatus status,
            LocalDateTime threshold
    );

    Page<EmailOutboxEntity> findAllByStatus(EmailOutboxStatus status, Pageable pageable);
}
