package org.example.rentathingproba.unit.email;

import org.example.rentathingproba.central.ListingAction;
import org.example.rentathingproba.central.ListingEvent;
import org.example.rentathingproba.notification.ListingNotificationService;
import org.example.rentathingproba.notification.handler.CreateListingHandler;
import org.example.rentathingproba.notification.handler.DeleteListingHandler;
import org.example.rentathingproba.notification.handler.ListingNotificationHandler;
import org.example.rentathingproba.notification.handler.UpdateListingHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListingNotificationService Unit Tests")
class ListingNotificationServiceTest {

    @Mock private CreateListingHandler createHandler;
    @Mock private UpdateListingHandler updateHandler;
    @Mock private DeleteListingHandler deleteHandler;

    private ListingNotificationService service;

    @BeforeEach
    void setUp() {
        when(createHandler.supports()).thenReturn(ListingAction.CREATE);
        when(updateHandler.supports()).thenReturn(ListingAction.UPDATE);
        when(deleteHandler.supports()).thenReturn(ListingAction.DELETE);

        List<ListingNotificationHandler> handlers = List.of(createHandler, updateHandler, deleteHandler);
        service = new ListingNotificationService(handlers);
    }

    @Test
    @DisplayName("notify: routes CREATE event only to CreateListingHandler")
    void shouldDelegateToCreateHandlerForCreateAction() {
        ListingEvent event = new ListingEvent(this, 1L, "user@test.com", "user", ListingAction.CREATE);

        service.notify(event);

        verify(createHandler).handle(event);
        verify(updateHandler, never()).handle(any());
        verify(deleteHandler, never()).handle(any());
    }

    @Test
    @DisplayName("notify: routes UPDATE event only to UpdateListingHandler")
    void shouldDelegateToUpdateHandlerForUpdateAction() {
        ListingEvent event = new ListingEvent(this, 2L, "user@test.com", "user", ListingAction.UPDATE);

        service.notify(event);

        verify(updateHandler).handle(event);
        verify(createHandler, never()).handle(any());
        verify(deleteHandler, never()).handle(any());
    }

    @Test
    @DisplayName("notify: routes DELETE event only to DeleteListingHandler")
    void shouldDelegateToDeleteHandlerForDeleteAction() {
        ListingEvent event = new ListingEvent(this, 3L, "user@test.com", "user", ListingAction.DELETE);

        service.notify(event);

        verify(deleteHandler).handle(event);
        verify(createHandler, never()).handle(any());
        verify(updateHandler, never()).handle(any());
    }

    @Test
    @DisplayName("notify: does not throw when no handler is registered for an action")
    void shouldDoNothingWhenNoHandlerRegistered() {
        ListingNotificationService emptyService = new ListingNotificationService(List.of());
        ListingEvent event = new ListingEvent(this, 99L, "user@test.com", "user", ListingAction.CREATE);

        emptyService.notify(event);
    }
}

