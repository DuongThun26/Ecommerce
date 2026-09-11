package com.example.ecommerce.config;

import com.example.ecommerce.dto.response.BaseResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.ZonedDateTime;

@RestControllerAdvice
public class ResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        // If already wrapped in BaseResponse, return as is
        if (body instanceof BaseResponse<?>) {
            return body;
        }

        // If response is void (null), return success response with null data
        if (body == null) {
            return BaseResponse.ok(null, ZonedDateTime.now());
        }

        // Wrap any other response in BaseResponse
        return BaseResponse.ok(body, ZonedDateTime.now());
    }
}
