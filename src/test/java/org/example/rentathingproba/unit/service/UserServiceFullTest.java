package org.example.rentathingproba.unit.service;

import org.example.rentathingproba.entities.Listing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.entities.UserFavourite;
import org.example.rentathingproba.exceptions.ListingNotFoundException;
import org.example.rentathingproba.exceptions.UserNotFoundException;
import org.example.rentathingproba.mapper.ListingMapper;
import org.example.rentathingproba.repository.ListingRepository;
import org.example.rentathingproba.repository.UserFavouriteRepository;
import org.example.rentathingproba.repository.UserRepository;
import org.example.rentathingproba.responses.ListingResponseDTO;
import org.example.rentathingproba.responses.UserResponseDTO;
import org.example.rentathingproba.service.UserService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests — full coverage")
class UserServiceFullTest {

    @Mock private UserRepository userRepository;
    @Mock private UserFavouriteRepository favouriteRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private ListingMapper listingMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private Listing listing;
    private ListingResponseDTO listingResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("dom")
                .email("dom@example.com")
                .password("enc")
                .enabled(true)
                .build();

        listing = new Listing();
        listing.setId(10L);
        listing.setUser(user);
        listing.setPrice(BigDecimal.valueOf(20));
        listing.setLocation("Zagreb");
        listing.setIsAvailable(true);
        listing.setCreatedAt(LocalDateTime.now());

        listingResponse = new ListingResponseDTO(10L, BigDecimal.valueOf(20), "Zagreb", true,
                LocalDateTime.now(), BigDecimal.ZERO, 5L, "Drill", "Tools", "A drill",
                List.of(), 1L, "dom");
    }

    // ── getProfile ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProfile: returns UserResponseDTO with computed average rating")
    void getProfile_returnsCorrectDTO() {
        user.setRatingSum(8.0);
        user.setRatingCount(2);
        when(favouriteRepository.findByUser(user)).thenReturn(List.of());
        when(listingRepository.findByUserId(1L)).thenReturn(List.of());

        UserResponseDTO result = userService.getProfile(user);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("dom");
        assertThat(result.getEmail()).isEqualTo("dom@example.com");
        // avg = 8.0 / 2 = 4.0
        assertThat(result.getRating()).isEqualTo(4.0);
        assertThat(result.getRatingCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("getProfile: returns zero average rating when user has no ratings")
    void getProfile_returnsZeroAverageWhenNoRatings() {
        user.setRatingSum(0.0);
        user.setRatingCount(0);
        when(favouriteRepository.findByUser(user)).thenReturn(List.of());
        when(listingRepository.findByUserId(1L)).thenReturn(List.of());

        UserResponseDTO result = userService.getProfile(user);

        assertThat(result.getRating()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getProfile: includes correct favourite count and listing count")
    void getProfile_includesCounts() {
        UserFavourite fav = new UserFavourite(user, listing);
        when(favouriteRepository.findByUser(user)).thenReturn(List.of(fav));
        when(listingRepository.findByUserId(1L)).thenReturn(List.of(listing));

        UserResponseDTO result = userService.getProfile(user);

        assertThat(result.getFavouriteCount()).isEqualTo(1);
        assertThat(result.getListingCount()).isEqualTo(1);
    }

    // ── getFavourites ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getFavourites: returns list of mapped ListingResponseDTOs")
    void getFavourites_returnsMappedList() {
        UserFavourite fav = new UserFavourite(user, listing);
        when(favouriteRepository.findByUser(user)).thenReturn(List.of(fav));
        when(listingMapper.toResponse(listing)).thenReturn(listingResponse);

        List<ListingResponseDTO> result = userService.getFavourites(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getListingId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getFavourites: returns empty list when user has no favourites")
    void getFavourites_returnsEmptyList() {
        when(favouriteRepository.findByUser(user)).thenReturn(List.of());

        List<ListingResponseDTO> result = userService.getFavourites(user);

        assertThat(result).isEmpty();
    }

    // ── addFavourite ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("addFavourite: saves favourite when listing exists and not already favourited")
    void addFavourite_savesWhenNotAlreadyFavourited() {
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
        when(favouriteRepository.existsByUserAndListing(user, listing)).thenReturn(false);

        userService.addFavourite(user, 10L);

        verify(favouriteRepository).save(any(UserFavourite.class));
    }

    @Test
    @DisplayName("addFavourite: does not save duplicate favourite")
    void addFavourite_doesNotSaveDuplicate() {
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
        when(favouriteRepository.existsByUserAndListing(user, listing)).thenReturn(true);

        userService.addFavourite(user, 10L);

        verify(favouriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("addFavourite: throws ListingNotFoundException when listing does not exist")
    void addFavourite_throwsWhenListingNotFound() {
        when(listingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.addFavourite(user, 99L))
                .isInstanceOf(ListingNotFoundException.class);
        verify(favouriteRepository, never()).save(any());
    }

    // ── removeFavourite ──────────────────────────────────────────────────────

    @Test
    @DisplayName("removeFavourite: calls deleteByUserAndListing when listing exists")
    void removeFavourite_deletesWhenListingExists() {
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));

        userService.removeFavourite(user, 10L);

        verify(favouriteRepository).deleteByUserAndListing(user, listing);
    }

    @Test
    @DisplayName("removeFavourite: throws ListingNotFoundException when listing does not exist")
    void removeFavourite_throwsWhenListingNotFound() {
        when(listingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.removeFavourite(user, 99L))
                .isInstanceOf(ListingNotFoundException.class);
        verify(favouriteRepository, never()).deleteByUserAndListing(any(), any());
    }

    // ── isFavourite ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("isFavourite: returns true when listing is in user's favourites")
    void isFavourite_returnsTrueWhenFavourited() {
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
        when(favouriteRepository.existsByUserAndListing(user, listing)).thenReturn(true);

        boolean result = userService.isFavourite(user, 10L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isFavourite: returns false when listing is not in user's favourites")
    void isFavourite_returnsFalseWhenNotFavourited() {
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
        when(favouriteRepository.existsByUserAndListing(user, listing)).thenReturn(false);

        boolean result = userService.isFavourite(user, 10L);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isFavourite: returns false when listing does not exist")
    void isFavourite_returnsFalseWhenListingNotFound() {
        when(listingRepository.findById(99L)).thenReturn(Optional.empty());

        boolean result = userService.isFavourite(user, 99L);

        assertThat(result).isFalse();
    }

    // ── rateUser ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("rateUser: adds score to ratingSum and increments ratingCount")
    void rateUser_updatesRatingFields() {
        user.setRatingSum(3.0);
        user.setRatingCount(1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.rateUser(1L, 4.5);

        assertThat(user.getRatingSum()).isEqualTo(7.5);
        assertThat(user.getRatingCount()).isEqualTo(2);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("rateUser: throws UserNotFoundException when target user does not exist")
    void rateUser_throwsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.rateUser(99L, 3.0))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).save(any());
    }
}