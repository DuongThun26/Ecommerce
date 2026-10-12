package com.example.ecommerce.report.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportParamErrorDto {
    private String param;
    private String reason;
    private String expected;

    public ReportParamErrorDto(String param, String reason) {
        this.param = param;
        this.reason = reason;
        this.expected = null;
    }
}
