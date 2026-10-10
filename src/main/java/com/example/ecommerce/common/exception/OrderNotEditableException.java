package com.example.ecommerce.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OrderNotEditableException extends RuntimeException {
    public OrderNotEditableException(String message) {
        super(message);
    }
}
