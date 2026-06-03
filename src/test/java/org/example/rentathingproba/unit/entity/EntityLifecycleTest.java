package org.example.rentathingproba.unit.entity;

import org.example.rentathingproba.entities.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Entity Lifecycle (@PrePersist) Unit Tests")
class EntityLifecycleTest {

    @Test
    @DisplayName("Booking.onCreate: sets createdAt and expiresAt when expiresAt is null")
    void booking_onCreate_setsCreatedAtAndDefaultExpiresAt() {
        Booking booking = new Booking();
        assertThat(booking.getExpiresAt()).isNull();

        invokePrePersist(booking);

        assertThat(booking.getCreatedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(booking.getExpiresAt()).isNotNull().isAfter(LocalDateTime.now().plusHours(23));
    }

    @Test
    @DisplayName("Booking.onCreate: does not overwrite expiresAt when already set")
    void booking_onCreate_doesNotOverwriteExistingExpiresAt() {
        Booking booking = new Booking();
        LocalDateTime customExpiry = LocalDateTime.now().plusDays(7);
        booking.setExpiresAt(customExpiry);

        invokePrePersist(booking);

        assertThat(booking.getExpiresAt()).isEqualTo(customExpiry);
    }

    @Test
    @DisplayName("Booking.onCreate: always sets createdAt regardless of expiresAt")
    void booking_onCreate_alwaysSetsCreatedAt() {
        Booking booking = new Booking();
        LocalDateTime customExpiry = LocalDateTime.now().plusHours(48);
        booking.setExpiresAt(customExpiry);

        invokePrePersist(booking);

        assertThat(booking.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Conversation.onCreate: sets createdAt timestamp")
    void conversation_onCreate_setsCreatedAt() {
        Conversation conversation = new Conversation();
        assertThat(conversation.getCreatedAt()).isNull();

        invokePrePersist(conversation);

        assertThat(conversation.getCreatedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("ChatMessage.onCreate: sets sentAt timestamp")
    void chatMessage_onCreate_setsSentAt() {
        ChatMessage message = new ChatMessage();
        assertThat(message.getSentAt()).isNull();

        invokePrePersist(message);

        assertThat(message.getSentAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Listing.onCreate: sets createdAt timestamp")
    void listing_onCreate_setsCreatedAt() {
        Listing listing = new Listing();
        assertThat(listing.getCreatedAt()).isNull();

        invokePrePersist(listing);

        assertThat(listing.getCreatedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Thing.onCreate: sets createdAt timestamp")
    void thing_onCreate_setsCreatedAt() {
        Thing thing = new Thing();
        assertThat(thing.getCreatedAt()).isNull();

        invokePrePersist(thing);

        assertThat(thing.getCreatedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("ListingImage: parameterised constructor sets all fields")
    void listingImage_parameterisedConstructor_setsAllFields() {
        Listing listing = new Listing();
        ListingImage image = new ListingImage(listing, "http://img.jpg", 2);

        assertThat(image.getListing()).isSameAs(listing);
        assertThat(image.getUrl()).isEqualTo("http://img.jpg");
        assertThat(image.getSortOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("ThingImage: parameterised constructor sets all fields")
    void thingImage_parameterisedConstructor_setsAllFields() {
        Thing thing = new Thing();
        ThingImage image = new ThingImage(thing, "http://thing-img.jpg", 1);

        assertThat(image.getThing()).isSameAs(thing);
        assertThat(image.getUrl()).isEqualTo("http://thing-img.jpg");
        assertThat(image.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("UserFavourite: parameterised constructor sets user and listing")
    void userFavourite_constructor_setsFields() {
        User user = User.builder().id(1L).build();
        Listing listing = new Listing();
        UserFavourite fav = new UserFavourite(user, listing);

        assertThat(fav.getUser()).isSameAs(user);
        assertThat(fav.getListing()).isSameAs(listing);
    }

    private void invokePrePersist(Object entity) {
        try {
            var method = entity.getClass().getDeclaredMethod("onCreate");
            method.setAccessible(true);
            method.invoke(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke @PrePersist on " + entity.getClass().getSimpleName(), e);
        }
    }
}