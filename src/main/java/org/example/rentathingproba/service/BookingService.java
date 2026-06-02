package org.example.rentathingproba.service;

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
import org.example.rentathingproba.responses.BookingDetailsDTO;
import org.example.rentathingproba.responses.BookingResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final SecureRandom PIN_RANDOM = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public BookingService(BookingRepository bookingRepository,
                          ListingRepository listingRepository,
                          ConversationRepository conversationRepository,
                          ChatMessageRepository chatMessageRepository) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public BookingResponseDTO createBookingRequest(CreateBookingDTO dto, User renter) {
        Listing listing = listingRepository.findById(dto.getListingId())
                .orElseThrow(() -> new ListingNotFoundException(dto.getListingId()));

        if (listing.getUser().getId().equals(renter.getId())) {
            throw new UnauthorizedException();
        }
        if (!listing.getIsAvailable()) {
            throw new IllegalStateException("This listing is currently not available for booking.");
        }

        LocalDate today = LocalDate.now();
        if (dto.getStartDate().isBefore(today)) {
            throw new IllegalArgumentException("Start date cannot be in the past.");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }

        boolean overlap = bookingRepository.existsOverlappingBooking(
                listing.getId(), dto.getStartDate(), dto.getEndDate());
        if (overlap) {
            throw new IllegalStateException(
                    "The selected dates overlap with an existing booking. Please choose different dates.");
        }

        long days = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        BigDecimal subtotal = listing.getPrice().multiply(BigDecimal.valueOf(days));
        BigDecimal deposit = listing.getSecurityDeposit() != null
                ? listing.getSecurityDeposit() : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(deposit);

        Conversation conversation = conversationRepository
                .findByListingIdAndBuyerId(listing.getId(), renter.getId())
                .orElseGet(() -> {
                    Conversation c = new Conversation();
                    c.setListing(listing);
                    c.setBuyer(renter);
                    return conversationRepository.save(c);
                });

        Booking booking = new Booking();
        booking.setListing(listing);
        booking.setRenter(renter);
        booking.setStartDate(dto.getStartDate());
        booking.setEndDate(dto.getEndDate());
        booking.setStatus(BookingStatus.PENDING);
        booking.setPricePerDay(listing.getPrice());
        booking.setTotalAmount(total);
        booking.setConversation(conversation);

        Booking saved = bookingRepository.save(booking);

        ChatMessage bookingMessage = new ChatMessage();
        bookingMessage.setConversation(conversation);
        bookingMessage.setSender(renter);
        bookingMessage.setContent("Booking Request: " + listing.getThings().getName());
        bookingMessage.setType(ChatMessage.MessageType.BOOKING_REQUEST);
        bookingMessage.setBooking(saved);
        chatMessageRepository.save(bookingMessage);

        log.info("Booking request created: id={}, listingId={}, renterId={}, expiresAt={}",
                saved.getId(), listing.getId(), renter.getId(), saved.getExpiresAt());

        return toResponse(saved);
    }

    public BookingResponseDTO confirmBooking(Long bookingId, User owner) {
        Booking booking = findAndAuthorizeOwner(bookingId, owner);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be confirmed.");
        }
        if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("This booking request has expired.");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPickupPin(generatePin());
        Booking saved = bookingRepository.save(booking);

        if (booking.getConversation() != null) {
            ChatMessage confirmMsg = new ChatMessage();
            confirmMsg.setConversation(booking.getConversation());
            confirmMsg.setSender(owner);
            confirmMsg.setContent("Booking Confirmed! The owner has accepted your request.");
            confirmMsg.setType(ChatMessage.MessageType.BOOKING_CONFIRMED);
            confirmMsg.setBooking(saved);
            chatMessageRepository.save(confirmMsg);
        }

        return toResponse(saved);
    }

    public BookingResponseDTO declineBooking(Long bookingId, User owner) {
        Booking booking = findAndAuthorizeOwner(bookingId, owner);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be declined.");
        }

        booking.setStatus(BookingStatus.DECLINED);
        Booking saved = bookingRepository.save(booking);

        if (booking.getConversation() != null) {
            ChatMessage declineMsg = new ChatMessage();
            declineMsg.setConversation(booking.getConversation());
            declineMsg.setSender(owner);
            declineMsg.setContent("Booking Declined");
            declineMsg.setType(ChatMessage.MessageType.BOOKING_DECLINED);
            declineMsg.setBooking(saved);
            chatMessageRepository.save(declineMsg);
        }

        return toResponse(saved);
    }

    public BookingResponseDTO cancelBooking(Long bookingId, User requestingUser) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        boolean isOwner = booking.getListing().getUser().getId().equals(requestingUser.getId());
        boolean isRenter = booking.getRenter().getId().equals(requestingUser.getId());
        if (!isOwner && !isRenter) {
            throw new UnauthorizedException();
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed booking.");
        }
        if (booking.getStatus() == BookingStatus.ACTIVE) {
            throw new IllegalStateException("Cannot cancel an active booking. Contact the other party.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);

        if (booking.getConversation() != null) {
            ChatMessage cancelMsg = new ChatMessage();
            cancelMsg.setConversation(booking.getConversation());
            cancelMsg.setSender(requestingUser);
            cancelMsg.setContent("Booking Cancelled");
            cancelMsg.setType(ChatMessage.MessageType.BOOKING_CANCELLED);
            cancelMsg.setBooking(saved);
            chatMessageRepository.save(cancelMsg);
        }

        return toResponse(saved);
    }

    public BookingResponseDTO confirmPickup(Long bookingId, String pin, User renter) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (!booking.getRenter().getId().equals(renter.getId())) {
            throw new UnauthorizedException();
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking must be confirmed before pickup.");
        }
        if (booking.getPickupPin() == null || !booking.getPickupPin().equals(pin)) {
            throw new IllegalArgumentException("Invalid PIN.");
        }

        booking.setStatus(BookingStatus.ACTIVE);
        Booking saved = bookingRepository.save(booking);

        if (booking.getConversation() != null) {
            ChatMessage pickupMsg = new ChatMessage();
            pickupMsg.setConversation(booking.getConversation());
            pickupMsg.setSender(renter);
            pickupMsg.setContent("Item picked up successfully!");
            pickupMsg.setType(ChatMessage.MessageType.PICKUP_CONFIRMED);
            pickupMsg.setBooking(saved);
            chatMessageRepository.save(pickupMsg);
        }

        return toResponse(saved);
    }

    public BookingResponseDTO confirmReturn(Long bookingId, User owner) {
        Booking booking = findAndAuthorizeOwner(bookingId, owner);
        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE bookings can be marked as returned.");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        Booking saved = bookingRepository.save(booking);

        if (booking.getConversation() != null) {
            ChatMessage returnMsg = new ChatMessage();
            returnMsg.setConversation(booking.getConversation());
            returnMsg.setSender(owner);
            returnMsg.setContent("Item returned successfully! Thank you!");
            returnMsg.setType(ChatMessage.MessageType.RETURN_CONFIRMED);
            returnMsg.setBooking(saved);
            chatMessageRepository.save(returnMsg);
        }

        return toResponse(saved);
    }

    public int expirePendingBookings() {
        List<Booking> expired = bookingRepository.findExpiredPendingBookings(LocalDateTime.now());
        for (Booking booking : expired) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            if (booking.getConversation() != null) {
                ChatMessage expiredMsg = new ChatMessage();
                expiredMsg.setConversation(booking.getConversation());
                expiredMsg.setSender(booking.getRenter());
                expiredMsg.setContent("Booking request expired after 24 hours");
                expiredMsg.setType(ChatMessage.MessageType.BOOKING_EXPIRED);
                expiredMsg.setBooking(booking);
                chatMessageRepository.save(expiredMsg);
            }

            log.info("Booking auto-expired: id={}, listingId={}", booking.getId(), booking.getListing().getId());
        }
        return expired.size();
    }

    @Transactional(readOnly = true)
    public String getPickupPin(Long bookingId, User requestingUser) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        boolean isOwner  = booking.getListing().getUser().getId().equals(requestingUser.getId());
        boolean isRenter = booking.getRenter().getId().equals(requestingUser.getId());
        if (!isOwner && !isRenter) {
            throw new UnauthorizedException();
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.ACTIVE) {
            throw new IllegalStateException("PIN is only available for CONFIRMED or ACTIVE bookings.");
        }

        return booking.getPickupPin();
    }

    @Transactional
    public void rateUserFromBooking(User reviewer, Long targetUserId, Long bookingId, double score) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // Only participants can rate
        boolean isOwner  = booking.getListing().getUser().getId().equals(reviewer.getId());
        boolean isRenter = booking.getRenter().getId().equals(reviewer.getId());
        if (!isOwner && !isRenter) {
            throw new UnauthorizedException();
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("You can only rate after the booking is completed.");
        }
        if (Boolean.TRUE.equals(booking.getReviewed())) {
            throw new IllegalStateException("You have already submitted a rating for this booking.");
        }
        if (score < 1.0 || score > 5.0) {
            throw new IllegalArgumentException("Score must be between 1 and 5.");
        }
        Long ownerUserId  = booking.getListing().getUser().getId();
        Long renterUserId = booking.getRenter().getId();
        boolean validTarget = targetUserId.equals(ownerUserId) || targetUserId.equals(renterUserId);
        if (!validTarget || targetUserId.equals(reviewer.getId())) {
            throw new UnauthorizedException();
        }
        booking.setReviewed(true);
        bookingRepository.save(booking);

        log.info("Rating applied: reviewerId={}, targetId={}, bookingId={}, score={}",
                reviewer.getId(), targetUserId, bookingId, score);
    }
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsForListing(Long listingId, User owner) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));
        if (!listing.getUser().getId().equals(owner.getId())) {
            throw new UnauthorizedException();
        }
        return bookingRepository.findByListingIdOrderByStartDateAsc(listingId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getMyBookings(User renter) {
        return bookingRepository.findByRenterIdOrderByStartDateDesc(renter.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BlockedPeriodDTO> getBlockedPeriods(Long listingId) {
        return bookingRepository.findActiveBookingsFrom(listingId, LocalDate.now())
                .stream()
                .map(b -> new BlockedPeriodDTO(b.getStartDate(), b.getEndDate()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isAvailable(Long listingId, LocalDate startDate, LocalDate endDate) {
        return !bookingRepository.existsOverlappingBooking(listingId, startDate, endDate);
    }

    private Booking findAndAuthorizeOwner(Long bookingId, User owner) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (!booking.getListing().getUser().getId().equals(owner.getId())) {
            throw new UnauthorizedException();
        }
        return booking;
    }

    private String generatePin() {
        int pin = PIN_RANDOM.nextInt(9000) + 1000;
        return String.valueOf(pin);
    }

    private BookingResponseDTO toResponse(Booking b) {
        return new BookingResponseDTO(
                b.getId(),
                b.getListing().getId(),
                b.getListing().getThings().getName(),
                b.getRenter().getId(),
                b.getRenter().getDisplayName(),
                b.getStartDate(),
                b.getEndDate(),
                b.getStatus(),
                b.getPricePerDay(),
                b.getTotalAmount(),
                b.getCreatedAt(),
                b.getExpiresAt(),
                null
        );
    }

    public BookingDetailsDTO toBookingDetailsDTO(Booking b, User viewer) {
        boolean isRenter = b.getRenter().getId().equals(viewer.getId());
        String firstImage = null;
        if (b.getListing().getImages() != null && !b.getListing().getImages().isEmpty()) {
            firstImage = b.getListing().getImages().get(0).getUrl();
        }
        long days = ChronoUnit.DAYS.between(b.getStartDate(), b.getEndDate()) + 1;
        BigDecimal deposit = b.getListing().getSecurityDeposit() != null
                ? b.getListing().getSecurityDeposit() : BigDecimal.ZERO;

        return new BookingDetailsDTO(
                b.getId(),
                b.getListing().getId(),
                b.getListing().getThings().getName(),
                firstImage,
                b.getStartDate(),
                b.getEndDate(),
                days,
                b.getPricePerDay(),
                deposit,
                b.getTotalAmount(),
                b.getStatus().name().toLowerCase(),
                null,
                b.getRenter().getDisplayName(),
                b.getListing().getUser().getDisplayName(),
                isRenter ? "renter" : "owner",
                b.getExpiresAt()
        );
    }
}