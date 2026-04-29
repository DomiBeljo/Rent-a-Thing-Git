package org.example.rentathingproba.exceptions;

public class ThingNotFoundException extends AppException {
    public ThingNotFoundException(Long id) {
        super(ErrorCode.THING_NOT_FOUND , "Thing with id " + id + " not found.");
    }
}
