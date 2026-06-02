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
import org.example.rentathingproba.responses.BlockedPeriodDTO;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Unit Tests")
class BookingServiceTest {

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
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner").email("owner@example.com").enabled(true).build();
        renter = User.builder().id(2L).username("renter").email("renter@example.com").enabled(true).build();

        thing = new Thing();
        thing.setId(10L);
        thing.setUser(owner);
        thing.setName("Drill");
        thing.setCategory("Tools");
        thing.setDescription("A drill");

        listing = new Listing();
        listing.setId(100L);
        listing.setUser(owner);
        listing.setThings(thing);
        listing.setPrice(BigDecimal.valueOf(20));
        listing.setSecurityDeposit(BigDecimal.valueOf(50));
        listing.setLocation("Zagreb");
        listing.setIsAvailable(true);

        conversation = new Conversation();
        conversation.setId(1L);
        conversation.setListing(listing);
        conversation.setBuyer(renter);

        pendingBooking = new Booking();
        pendingBooking.setId(200L);
        pendingBooking.setListing(listing);
        pendingBooking.setRenter(renter);
        pendingBooking.setStartDate(LocalDate.now().plusDays(1));
        pendingBooking.setEndDate(LocalDate.now().plusDays(3));
        pendingBooking.setStatus(BookingStatus.PENDING);
        pendingBooking.setPricePerDay(BigDecimal.valueOf(20));
        pendingBooking.setTotalAmount(BigDecimal.valueOf(110));
        pendingBooking.setCreatedAt(LocalDateTime.now());
        pendingBooking.setExpiresAt(LocalDateTime.now().plusHours(24));
        pendingBooking.setConversation(conversation);
    }

    @Test
    @DisplayName("createBookingRequest: throws ListingNotFoundException when listing does not exist")
    void createBookingRequest_throwsWhenListingNotFound() {
        CreateBookingDTO dto = buildDto(99L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        when(listingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBookingRequest(dto, renter))
                .isInstanceOf(ListingNotFoundException.class);
    }

    @Test
    @DisplayName("createBookingRequest: throws UnauthorizedException when owner tries to book own listing")
    void createBookingRequest_throwsWhenOwnerBooksOwnListing() {
        CreateBookingDTO dto = buildDto(100L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> bookingService.createBookingRequest(dto, owner))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("createBookingRequest: throws IllegalStateException when listing is not available")
    void createBookingRequest_throwsWhenListingUnavailable() {
        listing.setIsAvailable(false);
        CreateBookingDTO dto = buildDto(100L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> bookingService.createBookingRequest(dto, renter))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("createBookingRequest: throws IllegalArgumentException when start date is in the past")
    void createBookingRequest_throwsWhenStartDateInPast() {
        CreateBookingDTO dto = buildDto(100L, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3));
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> bookingService.createBookingRequest(dto, renter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past");
    }

    @Test
    @DisplayName("createBookingRequest: throws IllegalArgumentException when end date is before start date")
    void createBookingRequest_throwsWhenEndBeforeStart() {
        CreateBookingDTO dto = buildDto(100L, LocalDate.now().plusDays(5), LocalDate.now().plusDays(2));
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> bookingService.createBookingRequest(dto, renter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date");
    }

    @Test
    @DisplayName("createBookingRequest: throws IllegalStateException when dates overlap existing booking")
    void createBookingRequest_throwsWhenDatesOverlap() {
        CreateBookingDTO dto = buildDto(100L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> bookingService.createBookingRequest(dto, renter))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    @DisplayName("createBookingRequest: saves booking and chat message when valid")
    void createBookingRequest_savesBookingAndChatMessage() {
        CreateBookingDTO dto = buildDto(100L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(false);
        when(conversationRepository.findByListingIdAndBuyerId(100L, 2L)).thenReturn(Optional.of(conversation));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(200L);
            b.setCreatedAt(LocalDateTime.now());
            b.setExpiresAt(LocalDateTime.now().plusHours(24));
            return b;
        });
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.createBookingRequest(dto, renter);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(110));
        verify(bookingRepository).save(any(Booking.class));
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("createBookingRequest: creates new conversation when none exists")
    void createBookingRequest_createsConversationWhenNoneExists() {
        CreateBookingDTO dto = buildDto(100L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(false);
        when(conversationRepository.findByListingIdAndBuyerId(100L, 2L)).thenReturn(Optional.empty());
        when(conversationRepository.save(any())).thenReturn(conversation);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(201L);
            b.setCreatedAt(LocalDateTime.now());
            b.setExpiresAt(LocalDateTime.now().plusHours(24));
            return b;
        });
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bookingService.createBookingRequest(dto, renter);

        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    @DisplayName("confirmBooking: throws BookingNotFoundException when booking does not exist")
    void confirmBooking_throwsWhenNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirmBooking(999L, owner))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    @DisplayName("confirmBooking: throws UnauthorizedException when non-owner tries to confirm")
    void confirmBooking_throwsForNonOwner() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.confirmBooking(200L, renter))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("confirmBooking: throws IllegalStateException when booking is not PENDING")
    void confirmBooking_throwsWhenNotPending() {
        pendingBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.confirmBooking(200L, owner))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("confirmBooking: throws IllegalStateException when booking request has expired")
    void confirmBooking_throwsWhenExpired() {
        pendingBooking.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.confirmBooking(200L, owner))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("confirmBooking: sets status to CONFIRMED and generates 4-digit PIN")
    void confirmBooking_setsStatusAndGeneratesPin() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.confirmBooking(200L, owner);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(pendingBooking.getPickupPin()).isNotNull().hasSize(4);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("declineBooking: throws IllegalStateException when booking is not PENDING")
    void declineBooking_throwsWhenNotPending() {
        pendingBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.declineBooking(200L, owner))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("declineBooking: sets status to DECLINED and sends chat message")
    void declineBooking_setsStatusAndSendsMessage() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.declineBooking(200L, owner);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.DECLINED);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("cancelBooking: throws UnauthorizedException when unrelated user tries to cancel")
    void cancelBooking_throwsForUnrelatedUser() {
        User stranger = User.builder().id(99L).username("stranger").email("s@example.com").build();
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.cancelBooking(200L, stranger))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("cancelBooking: throws IllegalStateException when booking is COMPLETED")
    void cancelBooking_throwsWhenCompleted() {
        pendingBooking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.cancelBooking(200L, renter))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("completed");
    }

    @Test
    @DisplayName("cancelBooking: throws IllegalStateException when booking is ACTIVE")
    void cancelBooking_throwsWhenActive() {
        pendingBooking.setStatus(BookingStatus.ACTIVE);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.cancelBooking(200L, renter))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active");
    }

    @Test
    @DisplayName("cancelBooking: renter can cancel a PENDING booking")
    void cancelBooking_renterCanCancelPending() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.cancelBooking(200L, renter);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelBooking: owner can cancel a PENDING booking")
    void cancelBooking_ownerCanCancelPending() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.cancelBooking(200L, owner);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("confirmPickup: throws UnauthorizedException when non-renter tries to confirm pickup")
    void confirmPickup_throwsForNonRenter() {
        pendingBooking.setStatus(BookingStatus.CONFIRMED);
        pendingBooking.setPickupPin("1234");
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.confirmPickup(200L, "1234", owner))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("confirmPickup: throws IllegalStateException when booking is not CONFIRMED")
    void confirmPickup_throwsWhenNotConfirmed() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.confirmPickup(200L, "1234", renter))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("confirmed");
    }

    @Test
    @DisplayName("confirmPickup: throws IllegalArgumentException when PIN is invalid")
    void confirmPickup_throwsForWrongPin() {
        pendingBooking.setStatus(BookingStatus.CONFIRMED);
        pendingBooking.setPickupPin("9999");
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.confirmPickup(200L, "1234", renter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PIN");
    }

    @Test
    @DisplayName("confirmPickup: sets status to ACTIVE when correct PIN is provided")
    void confirmPickup_setsActiveWithCorrectPin() {
        pendingBooking.setStatus(BookingStatus.CONFIRMED);
        pendingBooking.setPickupPin("1234");
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.confirmPickup(200L, "1234", renter);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.ACTIVE);
    }

    @Test
    @DisplayName("confirmReturn: throws IllegalStateException when booking is not ACTIVE")
    void confirmReturn_throwsWhenNotActive() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.confirmReturn(200L, owner))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    @DisplayName("confirmReturn: sets status to COMPLETED and sends chat message")
    void confirmReturn_setsCompleted() {
        pendingBooking.setStatus(BookingStatus.ACTIVE);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.confirmReturn(200L, owner);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("expirePendingBookings: marks expired bookings as EXPIRED and returns count")
    void expirePendingBookings_expiresAndReturnsCount() {
        Booking expiredBooking = new Booking();
        expiredBooking.setId(300L);
        expiredBooking.setListing(listing);
        expiredBooking.setRenter(renter);
        expiredBooking.setStatus(BookingStatus.PENDING);
        expiredBooking.setExpiresAt(LocalDateTime.now().minusHours(1));
        expiredBooking.setConversation(conversation);

        when(bookingRepository.findExpiredPendingBookings(any())).thenReturn(List.of(expiredBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = bookingService.expirePendingBookings();

        assertThat(count).isEqualTo(1);
        assertThat(expiredBooking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("expirePendingBookings: returns 0 when no expired bookings found")
    void expirePendingBookings_returnsZeroWhenNoneExpired() {
        when(bookingRepository.findExpiredPendingBookings(any())).thenReturn(List.of());

        int count = bookingService.expirePendingBookings();

        assertThat(count).isEqualTo(0);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("getPickupPin: throws UnauthorizedException for unrelated user")
    void getPickupPin_throwsForUnrelatedUser() {
        pendingBooking.setStatus(BookingStatus.CONFIRMED);
        pendingBooking.setPickupPin("5678");
        User stranger = User.builder().id(99L).username("s").email("s@x.com").build();
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.getPickupPin(200L, stranger))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("getPickupPin: throws IllegalStateException when status is not CONFIRMED or ACTIVE")
    void getPickupPin_throwsWhenStatusInvalid() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.getPickupPin(200L, renter))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PIN");
    }

    @Test
    @DisplayName("getPickupPin: returns PIN for owner when booking is CONFIRMED")
    void getPickupPin_returnsPinForOwner() {
        pendingBooking.setStatus(BookingStatus.CONFIRMED);
        pendingBooking.setPickupPin("4321");
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        String pin = bookingService.getPickupPin(200L, owner);

        assertThat(pin).isEqualTo("4321");
    }

    @Test
    @DisplayName("rateUserFromBooking: throws IllegalStateException when booking is not COMPLETED")
    void rateUser_throwsWhenNotCompleted() {
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.rateUserFromBooking(renter, owner.getId(), 200L, 4.0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("completed");
    }

    @Test
    @DisplayName("rateUserFromBooking: throws IllegalStateException when booking already reviewed")
    void rateUser_throwsWhenAlreadyReviewed() {
        pendingBooking.setStatus(BookingStatus.COMPLETED);
        pendingBooking.setReviewed(true);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.rateUserFromBooking(renter, owner.getId(), 200L, 4.0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already");
    }

    @Test
    @DisplayName("rateUserFromBooking: throws IllegalArgumentException when score is out of range")
    void rateUser_throwsForInvalidScore() {
        pendingBooking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.rateUserFromBooking(renter, owner.getId(), 200L, 6.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Score");
    }

    @Test
    @DisplayName("rateUserFromBooking: throws UnauthorizedException when reviewer tries to rate themselves")
    void rateUser_throwsWhenRatingSelf() {
        pendingBooking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> bookingService.rateUserFromBooking(renter, renter.getId(), 200L, 4.0))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("rateUserFromBooking: marks booking as reviewed when valid")
    void rateUser_marksBookingAsReviewed() {
        pendingBooking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bookingService.rateUserFromBooking(renter, owner.getId(), 200L, 4.5);

        assertThat(pendingBooking.getReviewed()).isTrue();
        verify(bookingRepository).save(pendingBooking);
    }

    @Test
    @DisplayName("getBlockedPeriods: returns list of BlockedPeriodDTOs for active bookings")
    void getBlockedPeriods_returnsDTOs() {
        Booking active = new Booking();
        active.setStartDate(LocalDate.now().plusDays(2));
        active.setEndDate(LocalDate.now().plusDays(5));
        when(bookingRepository.findActiveBookingsFrom(eq(100L), any())).thenReturn(List.of(active));

        List<BlockedPeriodDTO> result = bookingService.getBlockedPeriods(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStartDate()).isEqualTo(active.getStartDate());
        assertThat(result.get(0).getEndDate()).isEqualTo(active.getEndDate());
    }

    @Test
    @DisplayName("isAvailable: returns true when no overlapping booking exists")
    void isAvailable_returnsTrueWhenNoOverlap() {
        when(bookingRepository.existsOverlappingBooking(100L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)))
                .thenReturn(false);

        boolean result = bookingService.isAvailable(100L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isAvailable: returns false when overlapping booking exists")
    void isAvailable_returnsFalseWhenOverlapExists() {
        when(bookingRepository.existsOverlappingBooking(100L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)))
                .thenReturn(true);

        boolean result = bookingService.isAvailable(100L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getMyBookings: returns bookings for the authenticated renter")
    void getMyBookings_returnsRenterBookings() {
        when(bookingRepository.findByRenterIdOrderByStartDateDesc(2L)).thenReturn(List.of(pendingBooking));

        List<BookingResponseDTO> result = bookingService.getMyBookings(renter);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRenterId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getBookingsForListing: throws UnauthorizedException when non-owner requests bookings")
    void getBookingsForListing_throwsForNonOwner() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> bookingService.getBookingsForListing(100L, renter))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("getBookingsForListing: returns bookings when requested by owner")
    void getBookingsForListing_returnsForOwner() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(bookingRepository.findByListingIdOrderByStartDateAsc(100L)).thenReturn(List.of(pendingBooking));

        List<BookingResponseDTO> result = bookingService.getBookingsForListing(100L, owner);

        assertThat(result).hasSize(1);
    }

    private CreateBookingDTO buildDto(Long listingId, LocalDate start, LocalDate end) {
        CreateBookingDTO dto = new CreateBookingDTO();
        dto.setListingId(listingId);
        dto.setStartDate(start);
        dto.setEndDate(end);
        return dto;
    }
}