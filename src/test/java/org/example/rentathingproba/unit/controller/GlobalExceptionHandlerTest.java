package org.example.rentathingproba.unit.controller;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.example.rentathingproba.controllers.GlobalExceptionHandler;
import org.example.rentathingproba.exceptions.*;
import org.example.rentathingproba.responses.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleUnauthorized: returns 401 with AUTH_UNAUTHORIZED code")
    void handleUnauthorized_returns401() {
        UnauthorizedException ex = new UnauthorizedException();

        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.AUTH_UNAUTHORIZED);
        assertThat(response.getBody().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("handleExpiredJwt: returns 401 with AUTH_TOKEN_EXPIRED code")
    void handleExpiredJwt_returns401() {
        ExpiredJwtException ex = new ExpiredJwtException(null, null, "expired");

        ResponseEntity<ErrorResponse> response = handler.handleExpiredJwt(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED);
        assertThat(response.getBody().getMessage()).contains("expired");
    }

    @Test
    @DisplayName("handleJwt: returns 401 with AUTH_TOKEN_INVALID code")
    void handleJwt_returns401() {
        JwtException ex = new JwtException("invalid token");

        ResponseEntity<ErrorResponse> response = handler.handleJwt(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
    }

    @Test
    @DisplayName("handleBadCredentials: returns 401 with AUTH_INVALID_CREDENTIALS code")
    void handleBadCredentials_returns401() {
        BadCredentialsException ex = new BadCredentialsException("bad creds");

        ResponseEntity<ErrorResponse> response = handler.handleBadCredentials(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid email or password.");
    }

    @Test
    @DisplayName("handleOwnership (ThingOwnershipException): returns 403 with THING_OWNERSHIP_REQUIRED code")
    void handleOwnership_thingOwnership_returns403() {
        ThingOwnershipException ex = new ThingOwnershipException();

        ResponseEntity<ErrorResponse> response = handler.handleOwnership(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.THING_OWNERSHIP_REQUIRED);
    }

    @Test
    @DisplayName("handleOwnership (ListingOwnershipException): returns 403 with LISTING_OWNERSHIP_REQUIRED code")
    void handleOwnership_listingOwnership_returns403() {
        ListingOwnershipException ex = new ListingOwnershipException();

        ResponseEntity<ErrorResponse> response = handler.handleOwnership(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.LISTING_OWNERSHIP_REQUIRED);
    }

    @Test
    @DisplayName("handleNotFoundEntity (UserNotFoundException): returns 404 with USER_NOT_FOUND code")
    void handleNotFoundEntity_user_returns404() {
        UserNotFoundException ex = new UserNotFoundException("ghost@example.com");

        ResponseEntity<ErrorResponse> response = handler.handleNotFoundEntity(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("handleNotFoundEntity (ThingNotFoundException): returns 404 with THING_NOT_FOUND code")
    void handleNotFoundEntity_thing_returns404() {
        ThingNotFoundException ex = new ThingNotFoundException(99L);

        ResponseEntity<ErrorResponse> response = handler.handleNotFoundEntity(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.THING_NOT_FOUND);
    }

    @Test
    @DisplayName("handleNotFoundEntity (ListingNotFoundException): returns 404 with LISTING_NOT_FOUND code")
    void handleNotFoundEntity_listing_returns404() {
        ListingNotFoundException ex = new ListingNotFoundException(42L);

        ResponseEntity<ErrorResponse> response = handler.handleNotFoundEntity(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.LISTING_NOT_FOUND);
    }

    @Test
    @DisplayName("handleNotFoundEntity (BookingNotFoundException): returns 404 with BOOKING_NOT_FOUND code")
    void handleNotFoundEntity_booking_returns404() {
        BookingNotFoundException ex = new BookingNotFoundException(200L);

        ResponseEntity<ErrorResponse> response = handler.handleNotFoundEntity(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.BOOKING_NOT_FOUND);
    }

    @Test
    @DisplayName("handleAuthBusinessRules (AccountNotVerifiedException): returns 409")
    void handleAuthBusinessRules_accountNotVerified_returns409() {
        AccountNotVerifiedException ex = new AccountNotVerifiedException();

        ResponseEntity<ErrorResponse> response = handler.handleAuthBusinessRules(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.AUTH_ACCOUNT_NOT_VERIFIED);
    }

    @Test
    @DisplayName("handleAuthBusinessRules (AccountAlreadyVerifiedException): returns 409")
    void handleAuthBusinessRules_accountAlreadyVerified_returns409() {
        AccountAlreadyVerifiedException ex = new AccountAlreadyVerifiedException();

        ResponseEntity<ErrorResponse> response = handler.handleAuthBusinessRules(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.AUTH_ACCOUNT_ALREADY_VERIFIED);
    }

    @Test
    @DisplayName("handleAuthBusinessRules (VerificationCodeExpiredException): returns 409")
    void handleAuthBusinessRules_verificationExpired_returns409() {
        VerificationCodeExpiredException ex = new VerificationCodeExpiredException();

        ResponseEntity<ErrorResponse> response = handler.handleAuthBusinessRules(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.AUTH_VERIFICATION_CODE_EXPIRED);
    }

    @Test
    @DisplayName("handleAuthBusinessRules (VerificationCodeInvalidException): returns 409")
    void handleAuthBusinessRules_verificationInvalid_returns409() {
        VerificationCodeInvalidException ex = new VerificationCodeInvalidException();

        ResponseEntity<ErrorResponse> response = handler.handleAuthBusinessRules(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.AUTH_VERIFICATION_CODE_INVALID);
    }

    @Test
    @DisplayName("handleValidation: returns 400 and includes field error details")
    void handleValidation_returns400WithDetails() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "email", "must not be blank");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.VALIDATION_INVALID_INPUT);
        assertThat(response.getBody().getMessage()).contains("email").contains("must not be blank");
    }

    @Test
    @DisplayName("handleIllegalState: returns 409 with BOOKING_INVALID_STATE code")
    void handleIllegalState_returns409() {
        IllegalStateException ex = new IllegalStateException("bad state");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalState(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.BOOKING_INVALID_STATE);
        assertThat(response.getBody().getMessage()).isEqualTo("bad state");
    }

    @Test
    @DisplayName("handleIllegalArgument: returns 400 with VALIDATION_INVALID_INPUT code")
    void handleIllegalArgument_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("bad arg");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.VALIDATION_INVALID_INPUT);
        assertThat(response.getBody().getMessage()).isEqualTo("bad arg");
    }

    @Test
    @DisplayName("handleGeneric: returns 500 with INTERNAL_ERROR code")
    void handleGeneric_returns500() {
        Exception ex = new RuntimeException("something went wrong");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred.");
    }

    @Test
    @DisplayName("build: ErrorResponse always includes a non-null timestamp")
    void build_errorResponseHasTimestamp() {
        UnauthorizedException ex = new UnauthorizedException();

        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(ex);

        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
}