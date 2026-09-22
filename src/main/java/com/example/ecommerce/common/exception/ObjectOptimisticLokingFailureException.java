package com.example.ecommerce.common.exception;

public class ObjectOptimisticLokingFailureException extends RuntimeException {
    public ObjectOptimisticLokingFailureException(String message) {
        super(message);
    }
}
