package com.example.ecommerce.mail.repository;

import com.example.ecommerce.mail.entity.EmailLogEntity;
import com.example.ecommerce.mail.enums.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLogEntity, Long>, JpaSpecificationExecutor<EmailLogEntity> {

    @Query("SELECT e FROM EmailLogEntity e WHERE e.status IN (:statuses) AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now) ORDER BY e.id ASC")
    List<EmailLogEntity> findReadyToProcessEmails(
            @Param("statuses") List<EmailStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    Page<EmailLogEntity> findByStatus(EmailStatus status, Pageable pageable);

    long countByStatus(EmailStatus status);
}
