package org.example.rentathingproba.integration;

import org.example.rentathingproba.dto.CreateBookingDTO;
import org.example.rentathingproba.dto.ListingDTO;
import org.example.rentathingproba.dto.ThingDTO;
import org.example.rentathingproba.entities.Booking;
import org.example.rentathingproba.entities.BookingStatus;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.exceptions.BookingNotFoundException;
import org.example.rentathingproba.exceptions.ListingNotFoundException;
import org.example.rentathingproba.exceptions.UnauthorizedException;
import org.example.rentathingproba.repository.BookingRepository;
import org.example.rentathingproba.repository.UserRepository;
import org.example.rentathingproba.responses.BlockedPeriodDTO;
import org.example.rentathingproba.responses.BookingResponseDTO;
import org.example.rentathingproba.responses.ListingResponseDTO;
import org.example.rentathingproba.responses.ThingResponseDTO;
import org.example.rentathingproba.service.BookingService;
import org.example.rentathingproba.service.ListingService;
import org.example.rentathingproba.service.ThingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("BookingService Integration Tests")
class BookingServiceIntegrationTest {

    @Autowired private BookingService bookingService;
    @Autowired private ThingService thingService;
    @Autowired private ListingService listingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;

    private User owner;
    private User renter;
    private ListingResponseDTO listing;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .username("booking_owner_int")
                .email("booking_owner_int@example.com")
                .password("encodedPassword")
                .enabled(true)
                .build());

        renter = userRepository.save(User.builder()
                .username("booking_renter_int")
                .email("booking_renter_int@example.com")
                .password("encodedPassword")
                .enabled(true)
                .build());

        ThingDTO thingDto = new ThingDTO();
        thingDto.setName("Drill");
        thingDto.setCategory("Tools");
        thingDto.setDescription("A drill for rent");
        thingDto.setImageUrls(List.of("img.jpg"));
        ThingResponseDTO thing = thingService.createThing(thingDto, owner);

        ListingDTO listingDto = new ListingDTO();
        listingDto.setThingId(thing.getThingId());
        listingDto.setPrice(BigDecimal.valueOf(20.00));
        listingDto.setLocation("Zagreb");
        listingDto.setSecurityDeposit(BigDecimal.valueOf(50.00));
        listing = listingService.createListing(listingDto, owner);
    }

    private CreateBookingDTO buildDto(LocalDate start, LocalDate end) {
        CreateBookingDTO dto = new CreateBookingDTO();
        dto.setListingId(listing.getListingId());
        dto.setStartDate(start);
        dto.setEndDate(end);
        return dto;
    }

    @Test
    @DisplayName("createBookingRequest: persists booking to database with PENDING status")
    void createBookingRequest_persistsToDatabase() {
        CreateBookingDTO dto = buildDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        BookingResponseDTO result = bookingService.createBookingRequest(dto, renter);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(result.getRenterId()).isEqualTo(renter.getId());
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(110));
        assertThat(bookingRepository.findById(result.getBookingId())).isPresent();
    }

    @Test
    @DisplayName("createBookingRequest: throws UnauthorizedException when owner books own listing")
    void createBookingRequest_ownerCannotBookOwnListing() {
        CreateBookingDTO dto = buildDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        assertThatThrownBy(() -> bookingService.createBookingRequest(dto, owner))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("createBookingRequest: throws when dates overlap an existing active booking")
    void createBookingRequest_throwsOnOverlappingDates() {
        CreateBookingDTO first = buildDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));
        bookingService.createBookingRequest(first, renter);

        CreateBookingDTO second = buildDto(LocalDate.now().plusDays(3), LocalDate.now().plusDays(7));
        assertThatThrownBy(() -> bookingService.createBookingRequest(second, renter))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    @DisplayName("confirmBooking: sets status to CONFIRMED and generates a 4-digit PIN in database")
    void confirmBooking_setsConfirmedAndPin() {
        BookingResponseDTO created = bookingService.createBookingRequest(
                buildDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)), renter);

        BookingResponseDTO confirmed = bookingService.confirmBooking(created.getBookingId(), owner);

        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        Booking inDb = bookingRepository.findById(created.getBookingId()).orElseThrow();
        assertThat(inDb.getPickupPin()).isNotNull().hasSize(4);
    }

    @Test
    @DisplayName("confirmBooking: throws UnauthorizedException when renter tries to confirm")
    void confirmBooking_throwsForRenter() {
        BookingResponseDTO created = bookingService.createBookingRequest(
                buildDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)), renter);

        assertThatThrownBy(() -> bookingService.confirmBooking(created.getBookingId(), renter))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("declineBooking: sets status to DECLINED in database")
    void declineBooking_setsDeclined() {
        BookingResponseDTO created = bookingService.createBookingRequest(
                buildDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)), renter);

        BookingResponseDTO declined = bookingService.declineBooking(created.getBookingId(), owner);

        assertThat(declined.getStatus()).isEqualTo(BookingStatus.DECLINED);
        Booking inDb = bookingRepository.findById(created.getBookingId()).orElseThrow();
        assertThat(inDb.getStatus()).isEqualTo(BookingStatus.DECLINED);
    }

    @Test
    @DisplayName("cancelBooking: renter can cancel a PENDING booking")
    void cancelBooking_renterCanCancelPending() {
        BookingResponseDTO created = bookingService.createBookingRequest(
                buildDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)), renter);

        BookingResponseDTO cancelled = bookingService.cancelBooking(created.getBookingId(), renter);

        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelBooking: throws BookingNotFoundException for non-existent booking")
    void cancelBooking_throwsWhenNotFound() {
        assertThatThrownBy(() -> bookingService.cancelBooking(999999L, renter))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    @DisplayName("confirmPickup: sets status to ACTIVE with correct PIN")
    void confirmPickup_setsActiveWithCorrectPin() {
        BookingResponseDTO created = bookingService.createBookingRequest(
                buildDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)), renter);
        bookingService.confirmBooking(created.getBookingId(), owner);

        String pin = bookingService.getPickupPin(created.getBookingId(), renter);
        BookingResponseDTO active = bookingService.confirmPickup(created.getBookingId(), pin, renter);

        assertThat(active.getStatus()).isEqualTo(BookingStatus.ACTIVE);
    }

    @Test
    @DisplayName("confirmPickup: throws IllegalArgumentException with wrong PIN")
    void confirmPickup_throwsWithWrongPin() {
        BookingResponseDTO created = bookingService.createBookingRequest(
                buildDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)), renter);
        bookingService.confirmBooking(created.getBookingId(), owner);

        assertThatThrownBy(() -> bookingService.confirmPickup(created.getBookingId(), "0000", renter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PIN");
    }

    @Test
    @DisplayName("confirmReturn: sets status to COMPLETED after full flow")
    void confirmReturn_setsCompleted() {
        BookingResponseDTO created = bookingService.createBookingRequest(
                buildDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)), renter);
        bookingService.confirmBooking(created.getBookingId(), owner);
        String pin = bookingService.getPickupPin(created.getBookingId(), renter);
        bookingService.confirmPickup(created.getBookingId(), pin, renter);

        BookingResponseDTO completed = bookingService.confirmReturn(created.getBookingId(), owner);

        assertThat(completed.getStatus()).isEqualTo(BookingStatus.COMPLETED);
    }

    @Test
    @DisplayName("getBlockedPeriods: returns blocked dates for the listing after a booking is created")
    void getBlockedPeriods_returnsBlockedDates() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(5);
        bookingService.createBookingRequest(buildDto(start, end), renter);

        List<BlockedPeriodDTO> blocked = bookingService.getBlockedPeriods(listing.getListingId());

        assertThat(blocked).hasSize(1);
        assertThat(blocked.get(0).getStartDate()).isEqualTo(start);
        assertThat(blocked.get(0).getEndDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("getMyBookings: returns only the renter's own bookings")
    void getMyBookings_returnsRenterBookings() {
        bookingService.createBookingRequest(
                buildDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)), renter);

        List<BookingResponseDTO> myBookings = bookingService.getMyBookings(renter);

        assertThat(myBookings).isNotEmpty();
        assertThat(myBookings).allSatisfy(b -> assertThat(b.getRenterId()).isEqualTo(renter.getId()));
    }

    @Test
    @DisplayName("getBookingsForListing: returns bookings for the listing when requested by owner")
    void getBookingsForListing_returnsForOwner() {
        bookingService.createBookingRequest(
                buildDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)), renter);

        List<BookingResponseDTO> bookings = bookingService.getBookingsForListing(listing.getListingId(), owner);

        assertThat(bookings).isNotEmpty();
    }

    @Test
    @DisplayName("getBookingsForListing: throws ListingNotFoundException for non-existent listing")
    void getBookingsForListing_throwsForUnknownListing() {
        assertThatThrownBy(() -> bookingService.getBookingsForListing(999999L, owner))
                .isInstanceOf(ListingNotFoundException.class);
    }
}