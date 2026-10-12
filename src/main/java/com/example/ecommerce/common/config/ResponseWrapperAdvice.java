package com.example.ecommerce.common.config;

import com.example.ecommerce.common.response.BaseResponse;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.ZonedDateTime;

@RestControllerAdvice
public class ResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !ByteArrayHttpMessageConverter.class.isAssignableFrom(converterType)
                && !StringHttpMessageConverter.class.isAssignableFrom(converterType)
                && !ResourceHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        String path = request.getURI().getPath();
        if (path.contains("/v3/api-docs") || path.contains("/swagger-ui")) {
            return body;
        }

        // If already wrapped in BaseResponse or ApiResponse, return as is
        if (body instanceof BaseResponse<?> || body instanceof com.example.ecommerce.common.response.ApiResponse<?>) {
            return body;
        }

        if (body instanceof Resource) {
            return body;
        }

        if (body instanceof com.example.ecommerce.order.dto.response.OrderItemsResponse) {
            return body;
        }

        if (body instanceof com.example.ecommerce.report.dto.ReportErrorResponse) {
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
