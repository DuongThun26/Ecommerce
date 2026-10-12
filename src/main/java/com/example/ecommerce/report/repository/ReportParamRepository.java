package com.example.ecommerce.report.repository;

import com.example.ecommerce.report.entity.ReportParamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportParamRepository extends JpaRepository<ReportParamEntity, Long> {
    List<ReportParamEntity> findByReportCodeOrderByDisplayOrderAsc(String reportCode);
}
