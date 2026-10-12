package com.example.ecommerce.report.service;

import com.example.ecommerce.report.dto.ReportDefinitionResponse;
import com.example.ecommerce.report.dto.ReportExportResult;

import java.util.Map;

public interface ReportService {

    ReportDefinitionResponse getReportDefinition(String reportCode);

    ReportExportResult exportReport(String reportCode, String format, Map<String, Object> inputParams);
}
