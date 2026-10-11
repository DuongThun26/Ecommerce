package com.example.ecommerce.common.exception;

public class IdempotencyPayloadMismatchException extends RuntimeException {
    public IdempotencyPayloadMismatchException(String message) {
        super(message);
    }
}
