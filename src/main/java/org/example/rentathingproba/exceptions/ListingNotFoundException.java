package org.example.rentathingproba.exceptions;

public class ListingNotFoundException extends AppException {
    public ListingNotFoundException(Long id) {
        super(ErrorCode.LISTING_NOT_FOUND, "Listing with id  " + id + " not found.");
    }
}
