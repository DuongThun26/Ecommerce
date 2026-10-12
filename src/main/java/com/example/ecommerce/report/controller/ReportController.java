package com.example.ecommerce.report.controller;

import com.example.ecommerce.report.dto.ReportDefinitionResponse;
import com.example.ecommerce.report.dto.ReportExportRequest;
import com.example.ecommerce.report.dto.ReportExportResult;
import com.example.ecommerce.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Report Management", description = "APIs định nghĩa ParamSet và xuất báo cáo JasperReports (PDF/XLSX)")
public class ReportController {

    private final ReportService reportService;

    /**
     * GET /api/v1/reports/{reportCode} → định nghĩa ParamSet
     */
    @Operation(summary = "Lấy định nghĩa ParamSet của báo cáo")
    @GetMapping("/{reportCode}")
    public ResponseEntity<ReportDefinitionResponse> getReportDefinition(@PathVariable("reportCode") String reportCode) {
        ReportDefinitionResponse response = reportService.getReportDefinition(reportCode);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/reports/{reportCode}/export → xuất file PDF hoặc XLSX
     */
    @Operation(summary = "Xuất báo cáo theo định dạng PDF hoặc XLSX")
    @PostMapping("/{reportCode}/export")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable("reportCode") String reportCode,
            @Valid @RequestBody ReportExportRequest request
    ) {
        ReportExportResult result = reportService.exportReport(
                reportCode,
                request.getFormat(),
                request.getParams()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(result.getContentType()));
        headers.setContentDispositionFormData("attachment", result.getFileName());
        headers.setContentLength(result.getContent().length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(result.getContent());
    }
}
