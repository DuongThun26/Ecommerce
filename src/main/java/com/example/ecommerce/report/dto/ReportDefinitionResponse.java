package com.example.ecommerce.report.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDefinitionResponse {
    private String code;
    private String name;
    private List<ReportParamResponse> params;
}
