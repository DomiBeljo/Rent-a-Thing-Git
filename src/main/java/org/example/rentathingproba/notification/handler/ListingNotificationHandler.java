package org.example.rentathingproba.notification.handler;

import org.example.rentathingproba.central.ListingAction;
import org.example.rentathingproba.central.ListingEvent;

public interface ListingNotificationHandler {
    ListingAction supports();
    void handle(ListingEvent event);
}
