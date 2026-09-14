package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ProductImportException extends RuntimeException {
    public ProductImportException(String message) {
        super(message);
    }

    public ProductImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
