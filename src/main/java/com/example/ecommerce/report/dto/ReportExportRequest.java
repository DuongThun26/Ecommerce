package com.example.ecommerce.report.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportExportRequest {

    @NotBlank(message = "Format báo cáo không được để trống (PDF hoặc XLSX)")
    private String format; // PDF or XLSX

    @Builder.Default
    private Map<String, Object> params = new HashMap<>();
}
