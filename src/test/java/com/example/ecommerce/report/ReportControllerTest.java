package com.example.ecommerce.report;

import com.example.ecommerce.common.config.ResponseWrapperAdvice;
import com.example.ecommerce.common.exception.GlobalExceptionHandler;
import com.example.ecommerce.common.exception.InvalidReportParamsException;
import com.example.ecommerce.report.controller.ReportController;
import com.example.ecommerce.report.dto.*;
import com.example.ecommerce.report.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ResponseWrapperAdvice.class})
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReportService reportService;

    @Test
    @DisplayName("GET /api/v1/reports/RPT_REVENUE_DAILY → trả định nghĩa ParamSet")
    void testGetReportDefinition() throws Exception {
        ReportDefinitionResponse response = ReportDefinitionResponse.builder()
                .code("RPT_REVENUE_DAILY")
                .name("BÁO CÁO DOANH THU THEO NGÀY")
                .params(List.of(
                        ReportParamResponse.builder().name("fromDate").dataType("DATE").required(true).formatPattern("yyyy-MM-dd").build(),
                        ReportParamResponse.builder().name("toDate").dataType("DATE").required(true).formatPattern("yyyy-MM-dd").build(),
                        ReportParamResponse.builder().name("customerId").dataType("LONG").required(false).build()
                ))
                .build();

        when(reportService.getReportDefinition("RPT_REVENUE_DAILY")).thenReturn(response);

        mockMvc.perform(get("/api/v1/reports/RPT_REVENUE_DAILY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("RPT_REVENUE_DAILY"))
                .andExpect(jsonPath("$.data.params[0].name").value("fromDate"))
                .andExpect(jsonPath("$.data.params[1].name").value("toDate"))
                .andExpect(jsonPath("$.data.params[2].name").value("customerId"));
    }

    @Test
    @DisplayName("POST /api/v1/reports/RPT_REVENUE_DAILY/export → trả file PDF thành công")
    void testExportPdfSuccess() throws Exception {
        byte[] fakePdf = "%PDF-1.4 Fake PDF Content".getBytes();
        ReportExportResult result = ReportExportResult.builder()
                .fileName("RPT_REVENUE_DAILY_20261001_20261004.pdf")
                .contentType("application/pdf")
                .content(fakePdf)
                .build();

        when(reportService.exportReport(eq("RPT_REVENUE_DAILY"), eq("PDF"), any())).thenReturn(result);

        ReportExportRequest request = ReportExportRequest.builder()
                .format("PDF")
                .params(Map.of("fromDate", "2026-10-01", "toDate", "2026-10-04"))
                .build();

        mockMvc.perform(post("/api/v1/reports/RPT_REVENUE_DAILY/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"RPT_REVENUE_DAILY_20261001_20261004.pdf\""))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(fakePdf));
    }

    @Test
    @DisplayName("Kịch bản 4: Lỗi params trả mọi lỗi trong 1 response 400 Bad Request")
    void testExportValidationErrorResponse() throws Exception {
        List<ReportParamErrorDto> errors = List.of(
                new ReportParamErrorDto("fromDate", "REQUIRED"),
                new ReportParamErrorDto("toDate", "INVALID_FORMAT", "yyyy-MM-dd")
        );

        when(reportService.exportReport(eq("RPT_REVENUE_DAILY"), eq("PDF"), any()))
                .thenThrow(new InvalidReportParamsException(errors));

        ReportExportRequest request = ReportExportRequest.builder()
                .format("PDF")
                .params(Map.of("toDate", "07/10/2026"))
                .build();

        mockMvc.perform(post("/api/v1/reports/RPT_REVENUE_DAILY/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REPORT_PARAMS"))
                .andExpect(jsonPath("$.message").value("Tham số báo cáo không hợp lệ"))
                .andExpect(jsonPath("$.errors[0].param").value("fromDate"))
                .andExpect(jsonPath("$.errors[0].reason").value("REQUIRED"))
                .andExpect(jsonPath("$.errors[1].param").value("toDate"))
                .andExpect(jsonPath("$.errors[1].reason").value("INVALID_FORMAT"))
                .andExpect(jsonPath("$.errors[1].expected").value("yyyy-MM-dd"));
    }
}
