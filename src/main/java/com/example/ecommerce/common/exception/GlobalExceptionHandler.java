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

    @ExceptionHandler(OrderNotEditableException.class)
    public ResponseEntity<BaseResponse<?>> handleOrderNotEditableException(OrderNotEditableException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail("ORDER_NOT_EDITABLE", ex.getMessage(), ZonedDateTime.now()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<BaseResponse<?>> handleInsufficientStockException(InsufficientStockException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail("INSUFFICIENT_STOCK", ex.getMessage(), ZonedDateTime.now()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ProductInactiveException.class)
    public ResponseEntity<BaseResponse<?>> handleProductInactiveException(ProductInactiveException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail("PRODUCT_INACTIVE", ex.getMessage(), ZonedDateTime.now()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(QuantityLimitExceededException.class)
    public ResponseEntity<BaseResponse<?>> handleQuantityLimitExceededException(QuantityLimitExceededException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail("QUANTITY_LIMIT_EXCEEDED", ex.getMessage(), ZonedDateTime.now()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ValidationErrorException.class)
    public ResponseEntity<BaseResponse<?>> handleValidationErrorException(ValidationErrorException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail("VALIDATION_ERROR", ex.getMessage(), ZonedDateTime.now()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(OrderConflictException.class)
    public ResponseEntity<BaseResponse<?>> handleOrderConflictException(OrderConflictException ex) {
        String code = ex.getErrorCode() != null ? ex.getErrorCode() : "CONCURRENT_MODIFICATION";
        return new ResponseEntity<>(
                BaseResponse.fail(code, ex.getMessage(), ZonedDateTime.now()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse<?>> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail("CONCURRENT_MODIFICATION", "Thao tác đồng thời dẫn đến xung đột dữ liệu.", ZonedDateTime.now()),
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
                .code("VALIDATION_ERROR")
                .message("Request contains invalid fields")
                .data(validationErrors)
                .error(ErrorResponse.builder()
                        .code("VALIDATION_ERROR")
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
                        "VALIDATION_ERROR",
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
                .success(false)
                .code("CONCURRENT_MODIFICATION")
                .message("Product or order was updated by another request. Please try again.")
                .error(ErrorResponse.builder()
                        .code("CONCURRENT_MODIFICATION")
                        .message("Product or order was updated by another request. Please try again.")
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

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<BaseResponse<?>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        return new ResponseEntity<>(
                BaseResponse.fail("FORBIDDEN", ex.getMessage() != null ? ex.getMessage() : "Bạn không có quyền thực hiện thao tác này.", ZonedDateTime.now()),
                HttpStatus.FORBIDDEN
        );
    }
}

