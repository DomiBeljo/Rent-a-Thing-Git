package org.example.rentathingproba.email.central;

import org.springframework.context.ApplicationEvent;

public class ListingEvent extends ApplicationEvent {

    private final Long listingId;
    private final String ownerEmail;
    private final String ownerUsername;
    private final ListingAction action;

    public ListingEvent(Object source, Long listingId, String ownerEmail, String ownerUsername, ListingAction action) {
        super(source);
        this.listingId = listingId;
        this.ownerEmail = ownerEmail;
        this.ownerUsername = ownerUsername;
        this.action = action;
    }

    public Long getListingId() {
        return listingId;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public ListingAction getAction() {
        return action;
    }
}
