package org.example.rentathingproba.central;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class ListingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ListingEventPublisher.class);

    private final ApplicationEventPublisher publisher;

    public ListingEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(Object source, Long listingId, String ownerEmail, String ownerUsername, ListingAction action) {
        log.debug("Publishing ListingEvent: listingId={}, action={}, owner={}", listingId, action, ownerEmail);
        publisher.publishEvent(new ListingEvent(source, listingId, ownerEmail, ownerUsername, action));
    }
}
