package org.example.rentathingproba.integration;

import org.example.rentathingproba.dto.ListingDTO;
import org.example.rentathingproba.dto.ThingDTO;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.exceptions.ListingNotFoundException;
import org.example.rentathingproba.repository.UserRepository;
import org.example.rentathingproba.responses.ListingResponseDTO;
import org.example.rentathingproba.responses.ThingResponseDTO;
import org.example.rentathingproba.responses.UserResponseDTO;
import org.example.rentathingproba.service.application.ListingService;
import org.example.rentathingproba.service.application.ThingService;
import org.example.rentathingproba.service.application.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("UserService Integration Tests")
class UserServiceIntegrationTest {

    @Autowired private UserService userService;
    @Autowired private ThingService thingService;
    @Autowired private ListingService listingService;
    @Autowired private UserRepository userRepository;

    private User user;
    private User otherUser;
    private ListingResponseDTO listing;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .username("userservice_user_int")
                .email("userservice_user_int@example.com")
                .password("encodedPassword")
                .enabled(true)
                .build());

        otherUser = userRepository.save(User.builder()
                .username("userservice_other_int")
                .email("userservice_other_int@example.com")
                .password("encodedPassword")
                .enabled(true)
                .build());

        ThingDTO thingDto = new ThingDTO();
        thingDto.setName("Bike");
        thingDto.setCategory("Transport");
        thingDto.setDescription("A bike");
        thingDto.setImageUrls(List.of("img.jpg"));
        ThingResponseDTO thing = thingService.createThing(thingDto, otherUser);

        ListingDTO listingDto = new ListingDTO();
        listingDto.setThingId(thing.getThingId());
        listingDto.setPrice(BigDecimal.valueOf(5.00));
        listingDto.setLocation("Split");
        listingDto.setSecurityDeposit(BigDecimal.valueOf(20.00));
        listing = listingService.createListing(listingDto, otherUser);
    }

    @Test
    @DisplayName("getProfile: returns user data with default rating values")
    void getProfile_returnsUserData() {
        UserResponseDTO profile = userService.getProfile(user);

        assertThat(profile.getId()).isEqualTo(user.getId());
        assertThat(profile.getUsername()).isEqualTo("userservice_user_int");
        assertThat(profile.getRating()).isEqualTo(0.0);
        assertThat(profile.getRatingCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("findAllUsers: returns list containing all persisted users")
    void findAllUsers_includesBothUsers() {
        List<UserResponseDTO> all = userService.findAllUsers();

        assertThat(all).extracting(UserResponseDTO::getId)
                .contains(user.getId(), otherUser.getId());
    }

    @Test
    @DisplayName("addFavourite: persists favourite and appears in getFavourites")
    void addFavourite_persistsAndAppearsInList() {
        userService.addFavourite(user, listing.getListingId());

        List<ListingResponseDTO> favs = userService.getFavourites(user);
        assertThat(favs).hasSize(1);
        assertThat(favs.get(0).getListingId()).isEqualTo(listing.getListingId());
    }

    @Test
    @DisplayName("addFavourite: is idempotent — adding same listing twice does not duplicate")
    void addFavourite_isIdempotent() {
        userService.addFavourite(user, listing.getListingId());
        userService.addFavourite(user, listing.getListingId());

        List<ListingResponseDTO> favs = userService.getFavourites(user);
        assertThat(favs).hasSize(1);
    }

    @Test
    @DisplayName("addFavourite: throws ListingNotFoundException for non-existent listing")
    void addFavourite_throwsForUnknownListing() {
        assertThatThrownBy(() -> userService.addFavourite(user, 999999L))
                .isInstanceOf(ListingNotFoundException.class);
    }

    @Test
    @DisplayName("isFavourite: returns true after adding favourite")
    void isFavourite_returnsTrueAfterAdding() {
        userService.addFavourite(user, listing.getListingId());

        assertThat(userService.isFavourite(user, listing.getListingId())).isTrue();
    }

    @Test
    @DisplayName("isFavourite: returns false before adding favourite")
    void isFavourite_returnsFalseBeforeAdding() {
        assertThat(userService.isFavourite(user, listing.getListingId())).isFalse();
    }

    @Test
    @DisplayName("removeFavourite: removes existing favourite from database")
    void removeFavourite_removesFavourite() {
        userService.addFavourite(user, listing.getListingId());
        assertThat(userService.isFavourite(user, listing.getListingId())).isTrue();

        userService.removeFavourite(user, listing.getListingId());

        assertThat(userService.isFavourite(user, listing.getListingId())).isFalse();
        assertThat(userService.getFavourites(user)).isEmpty();
    }

    @Test
    @DisplayName("rateUser: updates ratingSum and ratingCount in database")
    void rateUser_updatesRatingValues() {
        userService.rateUser(otherUser.getId(), 4.0);

        User updated = userRepository.findById(otherUser.getId()).orElseThrow();
        assertThat(updated.getRatingSum()).isEqualTo(4.0);
        assertThat(updated.getRatingCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("rateUser: accumulates multiple ratings correctly")
    void rateUser_accumulatesMultipleRatings() {
        userService.rateUser(otherUser.getId(), 4.0);
        userService.rateUser(otherUser.getId(), 5.0);

        User updated = userRepository.findById(otherUser.getId()).orElseThrow();
        assertThat(updated.getRatingSum()).isEqualTo(9.0);
        assertThat(updated.getRatingCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("getProfile: reflects updated average rating after rateUser")
    void getProfile_reflectsUpdatedRating() {
        userService.rateUser(otherUser.getId(), 4.0);
        userService.rateUser(otherUser.getId(), 5.0);

        User refreshed = userRepository.findById(otherUser.getId()).orElseThrow();
        UserResponseDTO profile = userService.getProfile(refreshed);

        assertThat(profile.getRating()).isEqualTo(4.5);
        assertThat(profile.getRatingCount()).isEqualTo(2);
    }
}