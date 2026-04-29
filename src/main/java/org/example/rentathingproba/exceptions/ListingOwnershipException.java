package org.example.rentathingproba.exceptions;

public class ListingOwnershipException extends AppException {
    public ListingOwnershipException() {
        super(ErrorCode.LISTING_OWNERSHIP_REQUIRED, "You can only manage your own listings.");
    }
}
