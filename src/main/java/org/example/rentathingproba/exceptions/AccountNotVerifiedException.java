package org.example.rentathingproba.exceptions;

public class AccountNotVerifiedException extends AppException {
    public AccountNotVerifiedException() {
        super(ErrorCode.AUTH_ACCOUNT_NOT_VERIFIED , "Account is not verified. Please check your email.");
    }
}
