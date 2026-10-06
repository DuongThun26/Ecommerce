package com.example.ecommerce.common.exception;

import com.example.ecommerce.common.response.BaseResponse;
import com.example.ecommerce.common.response.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
public class GlobalExceptionHandler {

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
                .error(ErrorResponse.builder()
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

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<BaseResponse<?>> handleOptimisticLocking(
            ObjectOptimisticLockingFailureException ex
    ) {
        BaseResponse<?> response = BaseResponse.builder()
                .error(ErrorResponse.builder()
                        .code("CONCURRENT_UPDATE")
                        .message("Product was updated by another request. Please try again.")
                        .build())
                .timestamp(ZonedDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BaseResponse<?>> handleBadRequestException(BadRequestException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail("BAD_REQUEST", ex.getMessage(), ZonedDateTime.now()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<BaseResponse<?>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail("FILE_TOO_LARGE", "Dung lượng file tải lên vượt quá giới hạn cho phép của hệ thống.", ZonedDateTime.now()),
                HttpStatus.PAYLOAD_TOO_LARGE
        );
    }
}

