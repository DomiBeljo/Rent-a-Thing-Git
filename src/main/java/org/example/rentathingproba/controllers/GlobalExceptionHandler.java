package org.example.rentathingproba.controllers;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.example.rentathingproba.exceptions.*;
import org.example.rentathingproba.responses.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 401
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized access attempt: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(ExpiredJwtException ex) {
        log.warn("Expired JWT token rejected.");
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_TOKEN_EXPIRED,
                "Your session has expired. Please login again.");
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwt(JwtException ex) {
        log.warn("Invalid JWT token rejected: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_TOKEN_INVALID,
                "Invalid authentication token. Please login again.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Failed login attempt: Bad credentials.");
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_INVALID_CREDENTIALS,
                "Invalid email or password.");
    }

    // 403
    @ExceptionHandler({ThingOwnershipException.class, ListingOwnershipException.class})
    public ResponseEntity<ErrorResponse> handleOwnership(AppException ex) {
        log.warn("Ownership violation: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getErrorCode(), ex.getMessage());
    }

    // 404
    @ExceptionHandler({UserNotFoundException.class, ThingNotFoundException.class,
            ListingNotFoundException.class, BookingNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundEntity(AppException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage());
    }

    // 409
    @ExceptionHandler({
            AccountNotVerifiedException.class,
            AccountAlreadyVerifiedException.class,
            VerificationCodeExpiredException.class,
            VerificationCodeInvalidException.class,
    })
    public ResponseEntity<ErrorResponse> handleAuthBusinessRules(AppException ex) {
        log.warn("Auth business rules violation: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage());
    }

    // 400 — Bean Validation (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", details);
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_INVALID_INPUT,
                "Validation failed: " + details);
    }

    // ✅ FIX 4a: IllegalStateException (booking state violations) → 409
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ErrorCode.BOOKING_INVALID_STATE, ex.getMessage());
    }

    // ✅ FIX 4b: IllegalArgumentException (bad input, invalid PIN) → 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_INVALID_INPUT, ex.getMessage());
    }

    // 500 — catches anything else
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred.");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, ErrorCode code, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), code, message));
    }
}