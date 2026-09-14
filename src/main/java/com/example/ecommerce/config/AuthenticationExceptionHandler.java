package com.example.ecommerce.config;

import com.example.ecommerce.dto.response.BaseResponse;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.exception.OrderProcessingException;
import com.example.ecommerce.exception.ProductImportException;
import com.example.ecommerce.exception.ResourceInUseException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class AuthenticationExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse<?>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail("RESOURCE_NOT_FOUND", ex.getMessage(), ZonedDateTime.now()),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<BaseResponse<?>> handleDuplicateResourceException(DuplicateResourceException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail("DUPLICATE_RESOURCE", ex.getMessage(), ZonedDateTime.now()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(OrderProcessingException.class)
    public ResponseEntity<BaseResponse<?>> handleOrderProcessingException(OrderProcessingException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail("ORDER_PROCESSING_FAILED", ex.getMessage(), ZonedDateTime.now()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ProductImportException.class)
    public ResponseEntity<BaseResponse<?>> handleProductImportException(ProductImportException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail("PRODUCT_IMPORT_FAILED", ex.getMessage(), ZonedDateTime.now()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ResourceInUseException.class)
    public ResponseEntity<BaseResponse<?>> handleResourceInUseException(ResourceInUseException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail("RESOURCE_IN_USE", ex.getMessage(), ZonedDateTime.now()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        // Create a response with validation error details
        BaseResponse<?> response = BaseResponse.builder()
                .success(false)
                .data(validationErrors)
                .error(com.example.ecommerce.dto.response.ErrorResponse.builder()
                        .code("VALIDATION_FAILED")
                        .message("Request contains invalid fields")
                        .build())
                .timestamp(ZonedDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail(
                        "INVALID_REQUEST_FORMAT",
                        "Malformed JSON or incorrect data type in request body",
                        ZonedDateTime.now()
                ),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<BaseResponse<?>> handleBadCredentialsException(BadCredentialsException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail("AUTHENTICATION_FAILED", ex.getMessage(), ZonedDateTime.now()),
                HttpStatus.UNAUTHORIZED
        );
    }
}

