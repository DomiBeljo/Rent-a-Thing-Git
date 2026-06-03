package org.example.rentathingproba.unit.service;

import org.example.rentathingproba.central.ListingEventPublisher;
import org.example.rentathingproba.entities.*;
import org.example.rentathingproba.exceptions.ListingNotFoundException;
import org.example.rentathingproba.mapper.ListingMapper;
import org.example.rentathingproba.repository.ListingRepository;
import org.example.rentathingproba.repository.ThingRepository;
import org.example.rentathingproba.repository.UserFavouriteRepository;
import org.example.rentathingproba.responses.ListingResponseDTO;
import org.example.rentathingproba.responses.MapMarkerDTO;
import org.example.rentathingproba.service.ListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListingService Extended Unit Tests")
class ListingServiceExtendedTest {

    @Mock private ListingRepository listingRepository;
    @Mock private ThingRepository thingRepository;
    @Mock private ListingMapper listingMapper;
    @Mock private ListingEventPublisher listingEventPublisher;
    @Mock private UserFavouriteRepository userFavouriteRepository;

    @InjectMocks
    private ListingService listingService;

    private User owner;
    private User otherUser;
    private Thing thing;
    private Listing listing;
    private ListingResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L).username("owner").email("owner@example.com")
                .ratingSum(0.0).ratingCount(0)
                .build();
        otherUser = User.builder()
                .id(2L).username("other").email("other@example.com")
                .ratingSum(8.0).ratingCount(2)
                .build();

        thing = new Thing();
        thing.setId(10L);
        thing.setUser(owner);
        thing.setName("Drill");
        thing.setCategory("Tools");
        thing.setImages(new ArrayList<>());

        listing = new Listing();
        listing.setId(100L);
        listing.setUser(owner);
        listing.setThings(thing);
        listing.setPrice(BigDecimal.valueOf(15));
        listing.setLocation("Zagreb");
        listing.setIsAvailable(true);
        listing.setCreatedAt(LocalDateTime.now());
        listing.setImages(new ArrayList<>());

        responseDTO = new ListingResponseDTO(100L, BigDecimal.valueOf(15), "Zagreb", true,
                LocalDateTime.now(), BigDecimal.valueOf(50), 10L, "Drill", "Tools",
                "A drill", List.of(), 1L, "owner");
    }

    @Test
    @DisplayName("getListingById: returns mapped DTO when listing exists")
    void getListingById_returnsResponse() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(listingMapper.toResponse(listing)).thenReturn(responseDTO);

        ListingResponseDTO result = listingService.getListingById(100L);

        assertThat(result).isEqualTo(responseDTO);
    }

    @Test
    @DisplayName("getListingById: throws ListingNotFoundException when listing does not exist")
    void getListingById_throwsWhenNotFound() {
        when(listingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listingService.getListingById(999L))
                .isInstanceOf(ListingNotFoundException.class);
    }

    @Test
    @DisplayName("getAllAvailableListingDTOs: returns all available listings as DTOs")
    void getAllAvailableListingDTOs_returnsMappedList() {
        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of(listing));
        when(listingMapper.toResponse(listing)).thenReturn(responseDTO);

        List<ListingResponseDTO> result = listingService.getAllAvailableListingDTOs();

        assertThat(result).hasSize(1).containsExactly(responseDTO);
    }

    @Test
    @DisplayName("getAllAvailableListingDTOs: returns empty list when no available listings exist")
    void getAllAvailableListingDTOs_returnsEmptyList() {
        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of());

        List<ListingResponseDTO> result = listingService.getAllAvailableListingDTOs();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getMapMarkers: returns markers for all available listings when category is null")
    void getMapMarkers_noCategoryFilter_returnsAllAvailable() {
        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of(listing));

        List<MapMarkerDTO> result = listingService.getMapMarkers(null, null);

        assertThat(result).hasSize(1);
        MapMarkerDTO marker = result.get(0);
        assertThat(marker.getListingId()).isEqualTo(100L);
        assertThat(marker.getName()).isEqualTo("Drill");
        assertThat(marker.getCategory()).isEqualTo("Tools");
        assertThat(marker.getLocation()).isEqualTo("Zagreb");
        assertThat(marker.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(15));
        assertThat(marker.getThumbnailUrl()).isNull();
        assertThat(marker.getIsAvailable()).isTrue();
        assertThat(marker.getUserId()).isEqualTo(1L);
        assertThat(marker.getUserRating()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getMapMarkers: returns markers for all available listings when category is blank")
    void getMapMarkers_blankCategory_returnsAllAvailable() {
        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of(listing));

        List<MapMarkerDTO> result = listingService.getMapMarkers("   ", null);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getMapMarkers: filters by category when category is provided")
    void getMapMarkers_withCategory_filtersResults() {
        when(listingRepository.findAvailableByCategory("Tools")).thenReturn(List.of(listing));

        List<MapMarkerDTO> result = listingService.getMapMarkers("Tools", null);

        assertThat(result).hasSize(1);
        verify(listingRepository).findAvailableByCategory("Tools");
        verify(listingRepository, never()).findAllAvailableWithThings();
    }

    @Test
    @DisplayName("getMapMarkers: excludes current user's own listings")
    void getMapMarkers_excludesCurrentUserListings() {
        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of(listing));

        List<MapMarkerDTO> result = listingService.getMapMarkers(null, owner);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getMapMarkers: includes listings when current user is not the owner")
    void getMapMarkers_includesListingsForOtherUser() {
        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of(listing));

        List<MapMarkerDTO> result = listingService.getMapMarkers(null, otherUser);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getMapMarkers: returns empty list when no listings are available")
    void getMapMarkers_returnsEmptyListWhenNoListings() {
        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of());

        List<MapMarkerDTO> result = listingService.getMapMarkers(null, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getMapMarkers: uses listing image as thumbnail when listing has images")
    void getMapMarkers_useListingImageAsThumbnail() {
        ListingImage img = new ListingImage(listing, "http://listing-img.jpg", 0);
        listing.setImages(List.of(img));

        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of(listing));

        List<MapMarkerDTO> result = listingService.getMapMarkers(null, null);

        assertThat(result.get(0).getThumbnailUrl()).isEqualTo("http://listing-img.jpg");
    }

    @Test
    @DisplayName("getMapMarkers: falls back to thing image when listing has no images but thing does")
    void getMapMarkers_fallsBackToThingImage() {
        listing.setImages(new ArrayList<>());

        ThingImage thingImg = new ThingImage(thing, "http://thing-img.jpg", 0);
        thing.setImages(List.of(thingImg));

        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of(listing));

        List<MapMarkerDTO> result = listingService.getMapMarkers(null, null);

        assertThat(result.get(0).getThumbnailUrl()).isEqualTo("http://thing-img.jpg");
    }

    @Test
    @DisplayName("getMapMarkers: thumbnail is null when neither listing nor thing has images")
    void getMapMarkers_thumbnailIsNullWhenNoImages() {
        listing.setImages(new ArrayList<>());
        thing.setImages(new ArrayList<>());

        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of(listing));

        List<MapMarkerDTO> result = listingService.getMapMarkers(null, null);

        assertThat(result.get(0).getThumbnailUrl()).isNull();
    }

    @Test
    @DisplayName("getMapMarkers: computes average rating correctly when owner has ratings")
    void getMapMarkers_computesAverageRatingForOwnerWithRatings() {
        listing.setUser(otherUser);
        thing.setUser(otherUser);

        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of(listing));

        List<MapMarkerDTO> result = listingService.getMapMarkers(null, null);

        assertThat(result.get(0).getUserRating()).isEqualTo(4.0);
    }

    @Test
    @DisplayName("getMapMarkers: average rating is 0.0 when owner has no ratings")
    void getMapMarkers_avgRatingIsZeroWhenOwnerHasNoRatings() {
        // owner has ratingCount=0
        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of(listing));

        List<MapMarkerDTO> result = listingService.getMapMarkers(null, null);

        assertThat(result.get(0).getUserRating()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getMapMarkers: thumbnail is null when listing.getImages() is null (not just empty)")
    void getMapMarkers_thumbnailIsNull_whenListingImagesIsNull() {
        listing.setImages(null);
        thing.setImages(null);
        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of(listing));

        List<MapMarkerDTO> result = listingService.getMapMarkers(null, null);

        assertThat(result.get(0).getThumbnailUrl()).isNull();
    }

    @Test
    @DisplayName("getMapMarkers: falls back to thing image when listing images null but thing has images")
    void getMapMarkers_fallsBackToThingImage_whenListingImagesIsNull() {
        listing.setImages(null);
        ThingImage thingImg = new ThingImage(thing, "http://thing-null-fallback.jpg", 0);
        thing.setImages(List.of(thingImg));

        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of(listing));

        List<MapMarkerDTO> result = listingService.getMapMarkers(null, null);

        assertThat(result.get(0).getThumbnailUrl()).isEqualTo("http://thing-null-fallback.jpg");
    }

    @Test
    @DisplayName("getMapMarkers: thumbnail is null when listing images null and thing images null")
    void getMapMarkers_thumbnailIsNull_whenBothImageListsAreNull() {
        listing.setImages(null);
        thing.setImages(null);

        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of(listing));

        List<MapMarkerDTO> result = listingService.getMapMarkers(null, null);

        assertThat(result.get(0).getThumbnailUrl()).isNull();
    }

    @Test
    @DisplayName("toMapMarker: thumbnail is null when getThings() returns null inside the else-if guard")
    void getMapMarkers_thumbnailIsNull_whenGetThingsNullInGuard() {
        Listing spyListing = spy(listing);

        doReturn(null).when(spyListing).getImages();

        doReturn(null).doReturn(thing).doReturn(thing).doReturn(thing)
                .when(spyListing).getThings();

        when(listingRepository.findAllAvailableWithThings()).thenReturn(List.of(spyListing));

        List<MapMarkerDTO> result = listingService.getMapMarkers(null, null);

        assertThat(result.get(0).getThumbnailUrl()).isNull();
        assertThat(result.get(0).getName()).isEqualTo("Drill");
    }
}