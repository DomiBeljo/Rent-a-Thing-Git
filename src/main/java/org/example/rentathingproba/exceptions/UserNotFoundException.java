package org.example.rentathingproba.exceptions;

public class UserNotFoundException extends AppException {
    public UserNotFoundException(String email) {
        super(ErrorCode.USER_NOT_FOUND, "User with email " + email + " not found.");
    }
}
