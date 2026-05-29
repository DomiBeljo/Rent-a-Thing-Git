package org.example.rentathingproba.unit.service;

import org.example.rentathingproba.dto.ListingDTO;
import org.example.rentathingproba.email.central.ListingAction;
import org.example.rentathingproba.email.central.ListingEventPublisher;
import org.example.rentathingproba.entities.Listing;
import org.example.rentathingproba.entities.Thing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.exceptions.ListingNotFoundException;
import org.example.rentathingproba.exceptions.ListingOwnershipException;
import org.example.rentathingproba.exceptions.ThingNotFoundException;
import org.example.rentathingproba.exceptions.ThingOwnershipException;
import org.example.rentathingproba.mapper.ListingMapper;
import org.example.rentathingproba.repository.ListingRepository;
import org.example.rentathingproba.repository.ThingRepository;
import org.example.rentathingproba.responses.ListingResponseDTO;
import org.example.rentathingproba.service.application.ListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListingService Unit Tests")
class ListingServiceTest {

    @Mock private ListingRepository listingRepository;
    @Mock private ThingRepository thingRepository;
    @Mock private ListingMapper listingMapper;
    @Mock private ListingEventPublisher listingEventPublisher;

    @InjectMocks
    private ListingService listingService;

    private User owner;
    private User otherUser;
    private Thing thing;
    private Listing listing;
    private ListingDTO dto;
    private ListingResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner").email("owner@example.com").build();
        otherUser = User.builder().id(2L).username("other").email("other@example.com").build();

        thing = new Thing();
        thing.setId(10L);
        thing.setUser(owner);
        thing.setName("Drill");
        thing.setCategory("Tools");

        listing = new Listing();
        listing.setId(100L);
        listing.setUser(owner);
        listing.setThings(thing);
        listing.setPrice(BigDecimal.valueOf(15));
        listing.setLocation("Zagreb");
        listing.setIsAvailable(true);
        listing.setCreatedAt(LocalDateTime.now());

        dto = new ListingDTO();
        dto.setThingId(10L);
        dto.setPrice(BigDecimal.valueOf(15));
        dto.setLocation("Zagreb");
        dto.setSecurityDeposit(BigDecimal.valueOf(50));

        responseDTO = new ListingResponseDTO(100L, BigDecimal.valueOf(15), "Zagreb", true,
                LocalDateTime.now(), BigDecimal.valueOf(50), 10L, "Drill", "Tools",
                "A drill", List.of(), 1L, "owner");
    }

    // ── createListing ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createListing: maps, saves, returns response, and publishes CREATE event")
    void createListing_savesAndPublishesCreateEvent() {
        when(thingRepository.findById(10L)).thenReturn(Optional.of(thing));
        when(listingMapper.toEntity(dto, owner, thing)).thenReturn(listing);
        when(listingRepository.save(listing)).thenReturn(listing);
        when(listingMapper.toResponse(listing)).thenReturn(responseDTO);

        ListingResponseDTO result = listingService.createListing(dto, owner);

        assertThat(result).isEqualTo(responseDTO);
        verify(listingRepository).save(listing);
        verify(listingEventPublisher).publish(any(), eq(100L), eq("owner@example.com"), eq("owner@example.com"), eq(ListingAction.CREATE));

    }

    @Test
    @DisplayName("createListing: throws ThingNotFoundException when thing does not exist")
    void createListing_throwsThingNotFound() {
        when(thingRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listingService.createListing(dto, owner))
                .isInstanceOf(ThingNotFoundException.class);
        verifyNoInteractions(listingRepository, listingEventPublisher);
    }

    @Test
    @DisplayName("createListing: throws ThingOwnershipException when user does not own the thing")
    void createListing_throwsThingOwnershipExceptionForNonOwner() {
        when(thingRepository.findById(10L)).thenReturn(Optional.of(thing));

        assertThatThrownBy(() -> listingService.createListing(dto, otherUser))
                .isInstanceOf(ThingOwnershipException.class);
        verifyNoInteractions(listingRepository, listingEventPublisher);
    }

    // ── updateListing ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateListing: updates, returns response, and publishes UPDATE event")
    void updateListing_updatesAndPublishesUpdateEvent() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(listing)).thenReturn(listing);
        when(listingMapper.toResponse(listing)).thenReturn(responseDTO);

        ListingResponseDTO result = listingService.updateListing(100L, dto, owner);

        assertThat(result).isEqualTo(responseDTO);
        verify(listingMapper).updateEntity(listing, dto);
        verify(listingRepository).save(listing);
        verify(listingEventPublisher).publish(any(), eq(100L), eq("owner@example.com"), eq("owner@example.com"), eq(ListingAction.UPDATE));
    }

    @Test
    @DisplayName("updateListing: throws ListingNotFoundException when listing does not exist")
    void updateListing_throwsListingNotFound() {
        when(listingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listingService.updateListing(999L, dto, owner))
                .isInstanceOf(ListingNotFoundException.class);
        verifyNoInteractions(listingEventPublisher);
    }

    @Test
    @DisplayName("updateListing: throws ListingOwnershipException for non-owner")
    void updateListing_throwsOwnershipExceptionForNonOwner() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> listingService.updateListing(100L, dto, otherUser))
                .isInstanceOf(ListingOwnershipException.class);
        verify(listingRepository, never()).save(any());
        verifyNoInteractions(listingEventPublisher);
    }

    // ── deleteListing ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteListing: deletes and publishes DELETE event for owner")
    void deleteListing_deletesAndPublishesDeleteEvent() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

        listingService.deleteListing(100L, owner);

        verify(listingRepository).delete(listing);
        verify(listingEventPublisher).publish(any(), eq(100L), eq("owner@example.com"), eq("owner@example.com"), eq(ListingAction.DELETE));
    }

    @Test
    @DisplayName("deleteListing: throws ListingNotFoundException when listing does not exist")
    void deleteListing_throwsListingNotFound() {
        when(listingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listingService.deleteListing(999L, owner))
                .isInstanceOf(ListingNotFoundException.class);
        verifyNoInteractions(listingEventPublisher);
    }

    @Test
    @DisplayName("deleteListing: throws ListingOwnershipException for non-owner")
    void deleteListing_throwsOwnershipExceptionForNonOwner() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> listingService.deleteListing(100L, otherUser))
                .isInstanceOf(ListingOwnershipException.class);
        verify(listingRepository, never()).delete(any());
        verifyNoInteractions(listingEventPublisher);
    }

    // ── isItAvailable ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("isItAvailable: toggles availability from true to false for owner")
    void isItAvailable_togglesTrueToFalse() {
        listing.setIsAvailable(true);
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

        listingService.isItAvailable(100L, owner);

        assertThat(listing.getIsAvailable()).isFalse();
        verify(listingRepository).save(listing);
    }

    @Test
    @DisplayName("isItAvailable: toggles availability from false to true for owner")
    void isItAvailable_togglesFalseToTrue() {
        listing.setIsAvailable(false);
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

        listingService.isItAvailable(100L, owner);

        assertThat(listing.getIsAvailable()).isTrue();
        verify(listingRepository).save(listing);
    }

    @Test
    @DisplayName("isItAvailable: throws ListingNotFoundException when listing does not exist")
    void isItAvailable_throwsListingNotFound() {
        when(listingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listingService.isItAvailable(999L, owner))
                .isInstanceOf(ListingNotFoundException.class);
    }

    @Test
    @DisplayName("isItAvailable: throws ListingOwnershipException for non-owner")
    void isItAvailable_throwsOwnershipExceptionForNonOwner() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> listingService.isItAvailable(100L, otherUser))
                .isInstanceOf(ListingOwnershipException.class);
        verify(listingRepository, never()).save(any());
    }

    // ── searchListing ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchListing: returns mapped results for matching query")
    void searchListing_returnsMappedResults() {
        when(listingRepository.findByTitle("drill")).thenReturn(List.of(listing));
        when(listingMapper.toResponse(listing)).thenReturn(responseDTO);

        List<ListingResponseDTO> results = listingService.searchListing("drill");

        assertThat(results).hasSize(1).containsExactly(responseDTO);
    }

    @Test
    @DisplayName("searchListing: returns empty list when no results match query")
    void searchListing_returnsEmptyListForNoMatch() {
        when(listingRepository.findByTitle("unknown")).thenReturn(List.of());

        List<ListingResponseDTO> results = listingService.searchListing("unknown");

        assertThat(results).isEmpty();
    }

    // ── getRecommended ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRecommended: returns mapped list of recommended listings")
    void getRecommended_returnsMappedRecommendedListings() {
        when(listingRepository.findRecommended()).thenReturn(List.of(listing));
        when(listingMapper.toResponse(listing)).thenReturn(responseDTO);

        List<ListingResponseDTO> results = listingService.getRecommended();

        assertThat(results).hasSize(1).containsExactly(responseDTO);
    }

    // ── getUserListing ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserListing: returns all listings for a given user")
    void getUserListing_returnsListingsForUser() {
        when(listingRepository.findByUserId(1L)).thenReturn(List.of(listing));
        when(listingMapper.toResponse(listing)).thenReturn(responseDTO);

        List<ListingResponseDTO> results = listingService.getUserListing(1L);

        assertThat(results).hasSize(1).containsExactly(responseDTO);
    }

    @Test
    @DisplayName("getUserListing: returns empty list when user has no listings")
    void getUserListing_returnsEmptyListForUserWithNoListings() {
        when(listingRepository.findByUserId(1L)).thenReturn(List.of());

        List<ListingResponseDTO> results = listingService.getUserListing(1L);

        assertThat(results).isEmpty();
    }
}
