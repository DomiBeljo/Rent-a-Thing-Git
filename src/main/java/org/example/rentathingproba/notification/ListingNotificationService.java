package org.example.rentathingproba.notification;

import org.example.rentathingproba.central.ListingAction;
import org.example.rentathingproba.central.ListingEvent;
import org.example.rentathingproba.notification.handler.ListingNotificationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ListingNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ListingNotificationService.class);

    private final Map<ListingAction, ListingNotificationHandler> handlerMap;

    public ListingNotificationService(List<ListingNotificationHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(ListingNotificationHandler::supports, Function.identity()));
        log.info("ListingNotificationService initialised with handlers: {}", handlerMap.keySet());
    }

    public void notify(ListingEvent event) {
        ListingNotificationHandler handler = handlerMap.get(event.getAction());
        if (handler == null) {
            log.warn("No handler registered for ListingAction={}", event.getAction());
            return;
        }
        handler.handle(event);
    }
}
