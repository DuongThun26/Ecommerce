package com.example.ecommerce.report.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportParamResponse {
    private String name;
    private String dataType;
    private boolean required;
    private String formatPattern;
    private String validationRules;
}
