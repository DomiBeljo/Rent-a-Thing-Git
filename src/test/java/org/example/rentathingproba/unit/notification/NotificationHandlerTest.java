package org.example.rentathingproba.unit.notification;

import jakarta.mail.MessagingException;
import org.example.rentathingproba.central.ListingAction;
import org.example.rentathingproba.central.ListingEvent;
import org.example.rentathingproba.notification.EmailService;
import org.example.rentathingproba.notification.ListingEmailTemplate;
import org.example.rentathingproba.notification.ListingEventListener;
import org.example.rentathingproba.notification.ListingNotificationService;
import org.example.rentathingproba.notification.handler.CreateListingHandler;
import org.example.rentathingproba.notification.handler.DeleteListingHandler;
import org.example.rentathingproba.notification.handler.UpdateListingHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Handler Unit Tests")
class NotificationHandlerTest {

    @Mock private EmailService emailService;
    @Mock private ListingEmailTemplate template;
    @Mock private ListingNotificationService notificationService;

    private CreateListingHandler createHandler;
    private UpdateListingHandler updateHandler;
    private DeleteListingHandler deleteHandler;
    private ListingEventListener eventListener;

    private ListingEvent createEvent;
    private ListingEvent updateEvent;
    private ListingEvent deleteEvent;

    @BeforeEach
    void setUp() {
        createHandler = new CreateListingHandler(emailService, template);
        updateHandler = new UpdateListingHandler(emailService, template);
        deleteHandler = new DeleteListingHandler(emailService, template);
        eventListener = new ListingEventListener(notificationService);

        createEvent = new ListingEvent(this, 1L, "owner@test.com", "ownerName", ListingAction.CREATE);
        updateEvent = new ListingEvent(this, 2L, "owner@test.com", "ownerName", ListingAction.UPDATE);
        deleteEvent = new ListingEvent(this, 3L, "owner@test.com", "ownerName", ListingAction.DELETE);
    }

    @Test
    @DisplayName("CreateListingHandler.supports: returns CREATE")
    void createHandler_supports_returnsCreate() {
        assertThat(createHandler.supports()).isEqualTo(ListingAction.CREATE);
    }

    @Test
    @DisplayName("CreateListingHandler.handle: sends email with create template")
    void createHandler_handle_sendsEmail() throws MessagingException {
        when(template.buildCreateHtml("ownerName", 1L)).thenReturn("<html>create</html>");

        createHandler.handle(createEvent);

        verify(template).buildCreateHtml("ownerName", 1L);
        verify(emailService).sendVerificationEmail(
                eq("owner@test.com"),
                contains("created"),
                eq("<html>create</html>")
        );
    }

    @Test
    @DisplayName("CreateListingHandler.handle: swallows MessagingException without rethrowing")
    void createHandler_handle_swallowsMessagingException() throws MessagingException {
        when(template.buildCreateHtml(anyString(), anyLong())).thenReturn("<html>");
        doThrow(MessagingException.class).when(emailService)
                .sendVerificationEmail(anyString(), anyString(), anyString());

        createHandler.handle(createEvent);
    }

    @Test
    @DisplayName("UpdateListingHandler.supports: returns UPDATE")
    void updateHandler_supports_returnsUpdate() {
        assertThat(updateHandler.supports()).isEqualTo(ListingAction.UPDATE);
    }

    @Test
    @DisplayName("UpdateListingHandler.handle: sends email with update template")
    void updateHandler_handle_sendsEmail() throws MessagingException {
        when(template.buildUpdateHtml("ownerName", 2L)).thenReturn("<html>update</html>");

        updateHandler.handle(updateEvent);

        verify(template).buildUpdateHtml("ownerName", 2L);
        verify(emailService).sendVerificationEmail(
                eq("owner@test.com"),
                contains("updated"),
                eq("<html>update</html>")
        );
    }

    @Test
    @DisplayName("UpdateListingHandler.handle: swallows MessagingException without rethrowing")
    void updateHandler_handle_swallowsMessagingException() throws MessagingException {
        when(template.buildUpdateHtml(anyString(), anyLong())).thenReturn("<html>");
        doThrow(MessagingException.class).when(emailService)
                .sendVerificationEmail(anyString(), anyString(), anyString());

        updateHandler.handle(updateEvent);
    }

    @Test
    @DisplayName("DeleteListingHandler.supports: returns DELETE")
    void deleteHandler_supports_returnsDelete() {
        assertThat(deleteHandler.supports()).isEqualTo(ListingAction.DELETE);
    }

    @Test
    @DisplayName("DeleteListingHandler.handle: sends email with delete template")
    void deleteHandler_handle_sendsEmail() throws MessagingException {
        when(template.buildDeleteHtml("ownerName", 3L)).thenReturn("<html>delete</html>");

        deleteHandler.handle(deleteEvent);

        verify(template).buildDeleteHtml("ownerName", 3L);
        verify(emailService).sendVerificationEmail(
                eq("owner@test.com"),
                contains("removed"),
                eq("<html>delete</html>")
        );
    }

    @Test
    @DisplayName("DeleteListingHandler.handle: swallows MessagingException without rethrowing")
    void deleteHandler_handle_swallowsMessagingException() throws MessagingException {
        when(template.buildDeleteHtml(anyString(), anyLong())).thenReturn("<html>");
        doThrow(MessagingException.class).when(emailService)
                .sendVerificationEmail(anyString(), anyString(), anyString());

        deleteHandler.handle(deleteEvent);
    }

    @Test
    @DisplayName("ListingEventListener.onListingEvent: delegates to notificationService.notify")
    void eventListener_delegatesToNotificationService() {
        eventListener.onListingEvent(createEvent);

        verify(notificationService).notify(createEvent);
    }

    @Test
    @DisplayName("ListingEventListener.onListingEvent: works for all action types")
    void eventListener_handlesAllActions() {
        eventListener.onListingEvent(updateEvent);
        eventListener.onListingEvent(deleteEvent);

        verify(notificationService).notify(updateEvent);
        verify(notificationService).notify(deleteEvent);
    }
}