package com.example.ecommerce.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class QuantityLimitExceededException extends RuntimeException {
    public QuantityLimitExceededException(String message) {
        super(message);
    }
}
