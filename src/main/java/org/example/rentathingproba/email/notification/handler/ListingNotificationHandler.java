package org.example.rentathingproba.email.notification.handler;

import org.example.rentathingproba.email.central.ListingAction;
import org.example.rentathingproba.email.central.ListingEvent;

public interface ListingNotificationHandler {

    ListingAction supports();

    void handle(ListingEvent event);
}
