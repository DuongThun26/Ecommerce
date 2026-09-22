package com.example.ecommerce.common.exception;

public class UsernameAlreadyExistsException extends DuplicateResourceException {
    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}
