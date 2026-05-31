package org.example.rentathingproba.exceptions;

public class BookingNotFoundException extends AppException {
    public BookingNotFoundException(Long id) {
        super(ErrorCode.BOOKING_NOT_FOUND, "Booking with id " + id + " not found.");
    }
}