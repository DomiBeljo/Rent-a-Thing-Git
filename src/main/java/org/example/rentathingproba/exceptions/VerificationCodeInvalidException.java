package org.example.rentathingproba.exceptions;

public class VerificationCodeInvalidException extends AppException {
    public VerificationCodeInvalidException() {
        super(ErrorCode.AUTH_VERIFICATION_CODE_INVALID, "Invalid verification code. Please try again.");
    }
}
