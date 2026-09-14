package com.example.ecommerce.exception;

public class ObjectOptimisticLokingFailureException extends RuntimeException {
    public ObjectOptimisticLokingFailureException(String message) {
        super(message);
    }
}
