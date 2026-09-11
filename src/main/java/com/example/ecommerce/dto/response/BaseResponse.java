package com.example.ecommerce.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {
    private boolean success;
    private T data;
    private ErrorResponse error;
    private ZonedDateTime timestamp;

    public static <T> BaseResponse<T> ok(T data) {
        return BaseResponse.<T>builder()
                .success(true)
                .data(data)
                .error(null)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    public static <T> BaseResponse<T> ok(T data, ZonedDateTime timestamp) {
        return BaseResponse.<T>builder()
                .success(true)
                .data(data)
                .error(null)
                .timestamp(timestamp)
                .build();
    }

    public static <T> BaseResponse<T> fail(String errorCode, String errorMessage) {
        return BaseResponse.<T>builder()
                .success(false)
                .data(null)
                .error(ErrorResponse.builder()
                        .code(errorCode)
                        .message(errorMessage)
                        .build())
                .timestamp(ZonedDateTime.now())
                .build();
    }

    public static <T> BaseResponse<T> fail(String errorCode, String errorMessage, ZonedDateTime timestamp) {
        return BaseResponse.<T>builder()
                .success(false)
                .data(null)
                .error(ErrorResponse.builder()
                        .code(errorCode)
                        .message(errorMessage)
                        .build())
                .timestamp(timestamp)
                .build();
    }
}
