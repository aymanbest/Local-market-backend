package com.localmarket.main.exception;

import org.springframework.http.HttpStatus;
    
public enum ErrorType {
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST),
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    ErrorType(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
} 