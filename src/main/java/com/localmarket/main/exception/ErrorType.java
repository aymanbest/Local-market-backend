package com.localmarket.main.exception;

import org.springframework.http.HttpStatus;
    
public enum ErrorType {
    // Resource errors
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT),
    RESOURCE_IN_USE(HttpStatus.CONFLICT),
    
    // Authentication/Authorization errors
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    INVALID_SESSION(HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    
    // Validation errors
    INVALID_REQUEST(HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    
    // Business logic errors
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST),
    OPERATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST),
    
    // Account related errors
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN),
    
    // Order related errors
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN),
    ORDER_INVALID_STATE(HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_PROCESSED(HttpStatus.CONFLICT),
    ORDER_CANCELLATION_FAILED(HttpStatus.BAD_REQUEST),
    
    // Product related errors
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PRODUCT_ACCESS_DENIED(HttpStatus.FORBIDDEN),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST),
    PRODUCT_ALREADY_EXISTS(HttpStatus.CONFLICT),
    INVALID_PRODUCT_PRICE(HttpStatus.BAD_REQUEST),
    
    // Category related errors
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND),
    CATEGORY_ALREADY_EXISTS(HttpStatus.CONFLICT),
    
    // Producer related errors
    PRODUCER_NOT_FOUND(HttpStatus.NOT_FOUND),
    PRODUCER_ALREADY_EXISTS(HttpStatus.CONFLICT),
    PRODUCER_ACCESS_DENIED(HttpStatus.FORBIDDEN),
    
    // File related errors
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST),
    
    // System errors
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorType(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
} 