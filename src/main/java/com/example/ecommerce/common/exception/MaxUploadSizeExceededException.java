package com.example.ecommerce.common.exception;

public class MaxUploadSizeExceededException extends RuntimeException {
    public MaxUploadSizeExceededException(String message) {
        super(message);
    }
}
