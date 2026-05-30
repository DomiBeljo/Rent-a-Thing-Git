package org.example.rentathingproba.service.application;

import org.example.rentathingproba.dto.CreateBookingDTO;
import org.example.rentathingproba.entities.Booking;
import org.example.rentathingproba.entities.Listing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.exceptions.ListingNotFoundException;
import org.example.rentathingproba.exceptions.UnauthorizedException;
import org.example.rentathingproba.repository.BookingRepository;
import org.example.rentathingproba.repository.ListingRepository;
import org.example.rentathingproba.responses.BlockedPeriodDTO;
import org.example.rentathingproba.responses.BookingResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;

    public BookingService(BookingRepository bookingRepository,
                          ListingRepository listingRepository) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
    }

    public BookingResponseDTO createBooking(CreateBookingDTO dto, User renter) {
        Listing listing = listingRepository.findById(dto.getListingId())
                .orElseThrow(() -> new ListingNotFoundException(dto.getListingId()));

        // Renters cannot book their own listings
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
        if (!dto.getEndDate().isAfter(dto.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date.");
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

        Booking booking = new Booking();
        booking.setListing(listing);
        booking.setRenter(renter);
        booking.setStartDate(dto.getStartDate());
        booking.setEndDate(dto.getEndDate());
        booking.setStatus(Booking.Status.PENDING);
        booking.setPricePerDay(listing.getPrice());
        booking.setTotalAmount(total);

        Booking saved = bookingRepository.save(booking);
        log.info("Booking created: id={}, listingId={}, renterId={}, dates={}/{}",
                saved.getId(), listing.getId(), renter.getId(),
                dto.getStartDate(), dto.getEndDate());
        return toResponse(saved);
    }

    public BookingResponseDTO confirmBooking(Long bookingId, User owner) {
        Booking booking = findAndAuthorizeOwner(bookingId, owner);
        if (booking.getStatus() != Booking.Status.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be confirmed.");
        }
        booking.setStatus(Booking.Status.CONFIRMED);
        return toResponse(bookingRepository.save(booking));
    }

    public BookingResponseDTO cancelBooking(Long bookingId, User requestingUser) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        boolean isOwner = booking.getListing().getUser().getId().equals(requestingUser.getId());
        boolean isRenter = booking.getRenter().getId().equals(requestingUser.getId());

        if (!isOwner && !isRenter) {
            throw new UnauthorizedException();
        }
        if (booking.getStatus() == Booking.Status.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed booking.");
        }

        booking.setStatus(Booking.Status.CANCELLED);
        return toResponse(bookingRepository.save(booking));
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

    // ── helpers ───────────────────────────────────────────────────────────────

    private Booking findAndAuthorizeOwner(Long bookingId, User owner) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        if (!booking.getListing().getUser().getId().equals(owner.getId())) {
            throw new UnauthorizedException();
        }
        return booking;
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
                b.getCreatedAt()
        );
    }
}
