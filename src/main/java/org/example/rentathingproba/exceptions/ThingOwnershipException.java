package org.example.rentathingproba.exceptions;

public class ThingOwnershipException extends AppException {
    public ThingOwnershipException() {
        super(ErrorCode.THING_OWNERSHIP_REQUIRED, "You can only manage your own things.");
    }
}
