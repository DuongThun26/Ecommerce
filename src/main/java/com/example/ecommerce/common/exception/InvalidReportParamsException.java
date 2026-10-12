package com.example.ecommerce.common.exception;

import com.example.ecommerce.report.dto.ReportParamErrorDto;
import lombok.Getter;

import java.util.List;

@Getter
public class InvalidReportParamsException extends RuntimeException {

    private final List<ReportParamErrorDto> errors;

    public InvalidReportParamsException(List<ReportParamErrorDto> errors) {
        super("Tham số báo cáo không hợp lệ");
        this.errors = errors;
    }
}
