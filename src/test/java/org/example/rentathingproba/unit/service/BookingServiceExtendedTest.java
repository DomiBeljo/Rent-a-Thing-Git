package org.example.rentathingproba.unit.service;

import org.example.rentathingproba.dto.CreateBookingDTO;
import org.example.rentathingproba.entities.*;
import org.example.rentathingproba.exceptions.BookingNotFoundException;
import org.example.rentathingproba.exceptions.ListingNotFoundException;
import org.example.rentathingproba.exceptions.UnauthorizedException;
import org.example.rentathingproba.repository.BookingRepository;
import org.example.rentathingproba.repository.ChatMessageRepository;
import org.example.rentathingproba.repository.ConversationRepository;
import org.example.rentathingproba.repository.ListingRepository;
import org.example.rentathingproba.responses.BookingDetailsDTO;
import org.example.rentathingproba.responses.BookingResponseDTO;
import org.example.rentathingproba.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Extended Unit Tests")
class BookingServiceExtendedTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private BookingService bookingService;

    private User owner;
    private User renter;
    private Thing thing;
    private Listing listing;
    private Booking pendingBooking;
    private Booking confirmedBooking;
    private Booking activeBooking;
    private Booking completedBooking;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner").email("owner@example.com").enabled(true).build();
        renter = User.builder().id(2L).username("renter").email("renter@example.com").enabled(true).build();

        thing = new Thing();
        thing.setId(10L);
        thing.setUser(owner);
        thing.setName("Drill");
        thing.setCategory("Tools");
        thing.setDescription("A power drill");

        listing = new Listing();
        listing.setId(100L);
        listing.setUser(owner);
        listing.setThings(thing);
        listing.setPrice(BigDecimal.valueOf(20));
        listing.setSecurityDeposit(BigDecimal.valueOf(50));
        listing.setLocation("Zagreb");
        listing.setIsAvailable(true);
        listing.setImages(new ArrayList<>());

        pendingBooking = buildBooking(200L, BookingStatus.PENDING, null);
        confirmedBooking = buildBooking(201L, BookingStatus.CONFIRMED, "5678");
        activeBooking = buildBooking(202L, BookingStatus.ACTIVE, "1234");
        completedBooking = buildBooking(203L, BookingStatus.COMPLETED, null);
    }

    private Booking buildBooking(Long id, BookingStatus status, String pin) {
        Booking b = new Booking();
        b.setId(id);
        b.setListing(listing);
        b.setRenter(renter);
        b.setStartDate(LocalDate.now().plusDays(1));
        b.setEndDate(LocalDate.now().plusDays(3));
        b.setStatus(status);
        b.setPricePerDay(BigDecimal.valueOf(20));
        b.setTotalAmount(BigDecimal.valueOf(110));
        b.setCreatedAt(LocalDateTime.now());
        b.setExpiresAt(LocalDateTime.now().plusHours(24));
        b.setPickupPin(pin);
        b.setConversation(null);
        return b;
    }

    @Test
    @DisplayName("confirmBooking: does not save chat message when booking has no conversation")
    void confirmBooking_noConversation_noMessageSaved() {
        pendingBooking.setConversation(null);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.confirmBooking(200L, owner);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("declineBooking: does not save chat message when booking has no conversation")
    void declineBooking_noConversation_noMessageSaved() {
        pendingBooking.setConversation(null);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.declineBooking(200L, owner);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.DECLINED);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("declineBooking: throws BookingNotFoundException when booking does not exist")
    void declineBooking_throwsWhenNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.declineBooking(999L, owner))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    @DisplayName("declineBooking: throws UnauthorizedException when non-owner tries to decline")
    void declineBooking_throwsForNonOwner() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.declineBooking(200L, renter))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("cancelBooking: does not save chat message when booking has no conversation")
    void cancelBooking_noConversation_noMessageSaved() {
        pendingBooking.setConversation(null);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.cancelBooking(200L, renter);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelBooking: throws BookingNotFoundException when booking does not exist")
    void cancelBooking_throwsWhenNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(999L, renter))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    @DisplayName("confirmPickup: does not save chat message when booking has no conversation")
    void confirmPickup_noConversation_noMessageSaved() {
        confirmedBooking.setConversation(null);
        when(bookingRepository.findById(201L)).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.confirmPickup(201L, "5678", renter);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.ACTIVE);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmPickup: throws BookingNotFoundException when booking does not exist")
    void confirmPickup_throwsWhenNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirmPickup(999L, "1234", renter))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    @DisplayName("confirmPickup: throws IllegalArgumentException when pickup pin is null")
    void confirmPickup_throwsWhenPinIsNull() {
        confirmedBooking.setPickupPin(null);
        when(bookingRepository.findById(201L)).thenReturn(Optional.of(confirmedBooking));

        assertThatThrownBy(() -> bookingService.confirmPickup(201L, "1234", renter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PIN");
    }

    @Test
    @DisplayName("confirmReturn: does not save chat message when booking has no conversation")
    void confirmReturn_noConversation_noMessageSaved() {
        activeBooking.setConversation(null);
        when(bookingRepository.findById(202L)).thenReturn(Optional.of(activeBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.confirmReturn(202L, owner);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmReturn: throws BookingNotFoundException when booking does not exist")
    void confirmReturn_throwsWhenNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirmReturn(999L, owner))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    @DisplayName("confirmReturn: throws UnauthorizedException when non-owner tries to confirm return")
    void confirmReturn_throwsForNonOwner() {
        when(bookingRepository.findById(202L)).thenReturn(Optional.of(activeBooking));

        assertThatThrownBy(() -> bookingService.confirmReturn(202L, renter))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("expirePendingBookings: expires booking without conversation — no chat message saved")
    void expirePendingBookings_noConversation_noMessageSaved() {
        Booking expiredNoConv = buildBooking(300L, BookingStatus.PENDING, null);
        expiredNoConv.setConversation(null);
        expiredNoConv.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(bookingRepository.findExpiredPendingBookings(any())).thenReturn(List.of(expiredNoConv));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = bookingService.expirePendingBookings();

        assertThat(count).isEqualTo(1);
        assertThat(expiredNoConv.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("getPickupPin: returns PIN for renter when booking is CONFIRMED")
    void getPickupPin_returnsPinForRenter() {
        confirmedBooking.setPickupPin("5678");
        when(bookingRepository.findById(201L)).thenReturn(Optional.of(confirmedBooking));

        String pin = bookingService.getPickupPin(201L, renter);

        assertThat(pin).isEqualTo("5678");
    }

    @Test
    @DisplayName("getPickupPin: returns PIN when booking status is ACTIVE")
    void getPickupPin_returnsPinForActiveBooking() {
        activeBooking.setPickupPin("1234");
        when(bookingRepository.findById(202L)).thenReturn(Optional.of(activeBooking));

        String pin = bookingService.getPickupPin(202L, owner);

        assertThat(pin).isEqualTo("1234");
    }

    @Test
    @DisplayName("getPickupPin: throws BookingNotFoundException when booking does not exist")
    void getPickupPin_throwsWhenNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getPickupPin(999L, owner))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    @DisplayName("rateUserFromBooking: throws UnauthorizedException for non-participant reviewer")
    void rateUser_throwsForNonParticipant() {
        User stranger = User.builder().id(99L).username("stranger").email("s@x.com").build();
        completedBooking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(203L)).thenReturn(Optional.of(completedBooking));

        assertThatThrownBy(() -> bookingService.rateUserFromBooking(stranger, owner.getId(), 203L, 4.0))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("rateUserFromBooking: throws IllegalArgumentException when score is below 1")
    void rateUser_throwsForScoreBelowOne() {
        completedBooking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(203L)).thenReturn(Optional.of(completedBooking));

        assertThatThrownBy(() -> bookingService.rateUserFromBooking(renter, owner.getId(), 203L, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Score");
    }

    @Test
    @DisplayName("rateUserFromBooking: owner can rate renter after completion")
    void rateUser_ownerRatesRenter() {
        completedBooking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(203L)).thenReturn(Optional.of(completedBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bookingService.rateUserFromBooking(owner, renter.getId(), 203L, 3.5);

        assertThat(completedBooking.getReviewed()).isTrue();
    }

    @Test
    @DisplayName("getBookingsForListing: throws ListingNotFoundException when listing does not exist")
    void getBookingsForListing_throwsWhenListingNotFound() {
        when(listingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingsForListing(999L, owner))
                .isInstanceOf(ListingNotFoundException.class);
    }

    @Test
    @DisplayName("toBookingDetailsDTO: renter role with no listing image — listingImage is null")
    void toBookingDetailsDTO_renterRole_noImage() {
        listing.setImages(new ArrayList<>());

        BookingDetailsDTO dto = bookingService.toBookingDetailsDTO(activeBooking, renter);

        assertThat(dto.getMyRole()).isEqualTo("renter");
        assertThat(dto.getListingImage()).isNull();
        assertThat(dto.getRenterName()).isEqualTo("renter");
        assertThat(dto.getOwnerName()).isEqualTo("owner");
        assertThat(dto.getStatus()).isEqualTo("active");
        assertThat(dto.getNumberOfDays()).isEqualTo(3L);
    }

    @Test
    @DisplayName("toBookingDetailsDTO: owner role with listing image present")
    void toBookingDetailsDTO_ownerRole_withImage() {
        ListingImage img = new ListingImage(listing, "http://img.jpg", 0);
        listing.setImages(List.of(img));

        BookingDetailsDTO dto = bookingService.toBookingDetailsDTO(activeBooking, owner);

        assertThat(dto.getMyRole()).isEqualTo("owner");
        assertThat(dto.getListingImage()).isEqualTo("http://img.jpg");
    }

    @Test
    @DisplayName("toBookingDetailsDTO: null security deposit is treated as zero")
    void toBookingDetailsDTO_nullDeposit_treatedAsZero() {
        listing.setSecurityDeposit(null);
        listing.setImages(new ArrayList<>());

        BookingDetailsDTO dto = bookingService.toBookingDetailsDTO(pendingBooking, renter);

        assertThat(dto.getDeposit()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("rateUserFromBooking: throws BookingNotFoundException when booking does not exist")
    void rateUser_throwsWhenBookingNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.rateUserFromBooking(renter, owner.getId(), 999L, 4.0))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    @DisplayName("rateUserFromBooking: throws UnauthorizedException when targetUserId is not a booking participant")
    void rateUser_throwsWhenTargetIsNotParticipant() {
        completedBooking.setStatus(BookingStatus.COMPLETED);
        Long strangerUserId = 99L;
        when(bookingRepository.findById(203L)).thenReturn(Optional.of(completedBooking));

        assertThatThrownBy(() -> bookingService.rateUserFromBooking(renter, strangerUserId, 203L, 4.0))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("createBookingRequest: treats null security deposit as zero in total amount calculation")
    void createBookingRequest_nullDeposit_treatedAsZero() {
        listing.setSecurityDeposit(null);
        CreateBookingDTO dto = new CreateBookingDTO();
        dto.setListingId(100L);
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(1));

        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(false);
        when(conversationRepository.findByListingIdAndBuyerId(100L, 2L)).thenReturn(Optional.of(
                new org.example.rentathingproba.entities.Conversation()));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(500L);
            b.setCreatedAt(LocalDateTime.now());
            b.setExpiresAt(LocalDateTime.now().plusHours(24));
            return b;
        });
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.createBookingRequest(dto, renter);

        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(20));
    }

    @Test
    @DisplayName("toBookingDetailsDTO: firstImage is null when listing.getImages() is null")
    void toBookingDetailsDTO_nullImagesList_firstImageIsNull() {
        listing.setImages(null);

        BookingDetailsDTO dto = bookingService.toBookingDetailsDTO(activeBooking, renter);

        assertThat(dto.getListingImage()).isNull();
    }
}