package com.example.ecommerce.report.repository;

import com.example.ecommerce.report.entity.ReportDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportDefinitionRepository extends JpaRepository<ReportDefinitionEntity, Long> {
    Optional<ReportDefinitionEntity> findByCode(String code);
}
