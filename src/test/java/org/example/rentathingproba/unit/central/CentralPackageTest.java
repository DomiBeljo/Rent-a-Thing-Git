package org.example.rentathingproba.unit.central;

import org.example.rentathingproba.central.ListingAction;
import org.example.rentathingproba.central.ListingEvent;
import org.example.rentathingproba.central.ListingEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Central Package Unit Tests")
class CentralPackageTest {

    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @InjectMocks private ListingEventPublisher listingEventPublisher;

    @Test
    @DisplayName("ListingEvent: all getters return the values passed to the constructor")
    void listingEvent_getters_returnConstructorValues() {
        Object source = new Object();
        ListingEvent event = new ListingEvent(source, 42L, "owner@test.com", "ownerName", ListingAction.CREATE);

        assertThat(event.getListingId()).isEqualTo(42L);
        assertThat(event.getOwnerEmail()).isEqualTo("owner@test.com");
        assertThat(event.getOwnerUsername()).isEqualTo("ownerName");
        assertThat(event.getAction()).isEqualTo(ListingAction.CREATE);
    }

    @Test
    @DisplayName("ListingEvent: getSource returns the source object passed to the constructor")
    void listingEvent_getSource_returnsSource() {
        Object source = this;
        ListingEvent event = new ListingEvent(source, 1L, "e@e.com", "u", ListingAction.DELETE);

        assertThat(event.getSource()).isSameAs(source);
    }

    @Test
    @DisplayName("ListingAction: all three enum values exist")
    void listingAction_allValuesExist() {
        assertThat(ListingAction.values()).containsExactlyInAnyOrder(
                ListingAction.CREATE, ListingAction.UPDATE, ListingAction.DELETE);
    }

    @Test
    @DisplayName("ListingAction: valueOf returns correct constant for each name")
    void listingAction_valueOf_returnsCorrectConstant() {
        assertThat(ListingAction.valueOf("CREATE")).isEqualTo(ListingAction.CREATE);
        assertThat(ListingAction.valueOf("UPDATE")).isEqualTo(ListingAction.UPDATE);
        assertThat(ListingAction.valueOf("DELETE")).isEqualTo(ListingAction.DELETE);
    }

    @Test
    @DisplayName("ListingEventPublisher.publish: publishes a ListingEvent with correct fields")
    void listingEventPublisher_publish_publishesCorrectEvent() {
        listingEventPublisher.publish(this, 99L, "owner@test.com", "ownerName", ListingAction.UPDATE);

        ArgumentCaptor<ListingEvent> captor = ArgumentCaptor.forClass(ListingEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        ListingEvent published = captor.getValue();
        assertThat(published.getListingId()).isEqualTo(99L);
        assertThat(published.getOwnerEmail()).isEqualTo("owner@test.com");
        assertThat(published.getOwnerUsername()).isEqualTo("ownerName");
        assertThat(published.getAction()).isEqualTo(ListingAction.UPDATE);
    }

    @Test
    @DisplayName("ListingEventPublisher.publish: works for all action types")
    void listingEventPublisher_publish_worksForAllActions() {
        for (ListingAction action : ListingAction.values()) {
            listingEventPublisher.publish(this, 1L, "e@e.com", "user", action);
        }

        ArgumentCaptor<ListingEvent> captor = ArgumentCaptor.forClass(ListingEvent.class);
        verify(applicationEventPublisher, org.mockito.Mockito.times(3))
                .publishEvent(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(ListingEvent::getAction)
                .containsExactlyInAnyOrder(ListingAction.CREATE, ListingAction.UPDATE, ListingAction.DELETE);
    }
}