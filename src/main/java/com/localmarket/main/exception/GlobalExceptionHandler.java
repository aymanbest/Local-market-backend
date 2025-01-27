package com.localmarket.main.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.localmarket.main.dto.error.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.ConstraintViolationException;


@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException ex, 
            HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .status(ex.getErrorType().getStatus().value())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, ex.getErrorType().getStatus());
    }

    @ExceptionHandler({
        BadCredentialsException.class,
        UsernameNotFoundException.class,
        AuthenticationCredentialsNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            Exception ex, 
            HttpServletRequest request) {
        ApiException apiEx = new ApiException(ErrorType.INVALID_CREDENTIALS, ex.getMessage());
        return handleApiException(apiEx, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, 
            HttpServletRequest request) {
        ApiException apiEx = new ApiException(ErrorType.ACCESS_DENIED, "Access denied to this resource");
        return handleApiException(apiEx, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        ApiException apiEx = new ApiException(ErrorType.DUPLICATE_RESOURCE, "Resource already exists");
        return handleApiException(apiEx, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce("", (a, b) -> a + "; " + b);
        ApiException apiEx = new ApiException(ErrorType.VALIDATION_FAILED, message);
        return handleApiException(apiEx, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        ApiException apiEx = new ApiException(ErrorType.VALIDATION_FAILED, ex.getMessage());
        return handleApiException(apiEx, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex, 
            HttpServletRequest request) {
        // ApiException apiEx = new ApiException(ErrorType.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        // FOR MORE DETAILS ON THE ERROR
        ex.printStackTrace();
        
        ApiException apiEx = new ApiException(ErrorType.INTERNAL_SERVER_ERROR, 
            "An unexpected error occurred: " + ex.getMessage());
        return handleApiException(apiEx, request);
    }
} 