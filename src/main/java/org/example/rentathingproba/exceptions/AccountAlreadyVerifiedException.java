package org.example.rentathingproba.exceptions;

public class AccountAlreadyVerifiedException extends AppException {
    public AccountAlreadyVerifiedException() {
        super(ErrorCode.AUTH_ACCOUNT_ALREADY_VERIFIED, "Account has already been verified.");
    }
}