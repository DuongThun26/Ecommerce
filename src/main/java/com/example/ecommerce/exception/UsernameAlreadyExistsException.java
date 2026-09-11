package com.example.ecommerce.exception;

public class UsernameAlreadyExistsException extends DuplicateResourceException {
    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}
