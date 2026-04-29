package org.example.rentathingproba.exceptions;

public class VerificationCodeExpiredException extends AppException {
    public VerificationCodeExpiredException() {
        super(ErrorCode.AUTH_VERIFICATION_CODE_EXPIRED, "Verification code has expired. Please try again.");
    }
}
