package org.example.rentathingproba.email.notification;

import org.example.rentathingproba.email.central.ListingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ListingEventListener {

    private static final Logger log = LoggerFactory.getLogger(ListingEventListener.class);

    private final ListingNotificationService notificationService;

    public ListingEventListener(ListingNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async
    @EventListener
    public void onListingEvent(ListingEvent event) {
        log.debug("Received ListingEvent async: listingId={}, action={}", event.getListingId(), event.getAction());
        notificationService.notify(event);
    }
}
