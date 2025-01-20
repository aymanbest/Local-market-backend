package com.localmarket.main.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final ErrorType errorType;

    public ApiException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }
} 