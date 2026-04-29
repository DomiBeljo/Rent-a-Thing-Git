package org.example.rentathingproba.exceptions;

public class UnauthorizedException extends AppException {
    public UnauthorizedException() {
        super(ErrorCode.AUTH_UNAUTHORIZED , "You are not authorised to perform this action.");
    }
}
