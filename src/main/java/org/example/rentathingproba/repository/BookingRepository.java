package org.example.rentathingproba.repository;

import org.example.rentathingproba.entities.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ── Lista bookinga za listing (owner view) ─────────────────────────────
    // JOIN FETCH renter i listing.things jer ih toResponse() koristi
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.listing l
        JOIN FETCH l.things
        JOIN FETCH l.user
        JOIN FETCH b.renter
        WHERE b.listing.id = :listingId
        ORDER BY b.startDate ASC
    """)
    List<Booking> findByListingIdOrderByStartDateAsc(@Param("listingId") Long listingId);

    // ── Moji bookings (renter view) ────────────────────────────────────────
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.listing l
        JOIN FETCH l.things
        JOIN FETCH l.user
        JOIN FETCH b.renter
        LEFT JOIN FETCH l.images
        WHERE b.renter.id = :renterId
        ORDER BY b.startDate DESC
    """)
    List<Booking> findByRenterIdOrderByStartDateDesc(@Param("renterId") Long renterId);

    // ── Detalji jednog bookinga ────────────────────────────────────────────
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.listing l
        JOIN FETCH l.things
        JOIN FETCH l.user
        JOIN FETCH b.renter
        LEFT JOIN FETCH l.images
        LEFT JOIN FETCH b.conversation
        WHERE b.id = :id
    """)
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    // ── Provjera overlapping bookinga ─────────────────────────────────────
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.listing.id = :listingId
        AND b.status IN ('CONFIRMED', 'PENDING', 'ACTIVE')
        AND b.startDate <= :endDate
        AND b.endDate   >= :startDate
    """)
    boolean existsOverlappingBooking(
            @Param("listingId") Long listingId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);

    // ── Zauzeti periodi za kalendar ────────────────────────────────────────
    @Query("""
        SELECT b FROM Booking b
        WHERE b.listing.id = :listingId
        AND b.status IN ('CONFIRMED', 'PENDING', 'ACTIVE')
        AND b.endDate >= :from
        ORDER BY b.startDate ASC
    """)
    List<Booking> findActiveBookingsFrom(
            @Param("listingId") Long listingId,
            @Param("from")      LocalDate from);

    // ── BULK expire ────────────────────────────────────────────────────────
    // Staro: findExpiredPendingBookings + for-loop save() = N UPDATE querya
    // Novo: jedan UPDATE query za sve, + chat poruke u odvojenom koraku
    @Modifying
    @Query("""
        UPDATE Booking b
        SET b.status = 'EXPIRED'
        WHERE b.status = 'PENDING'
        AND b.expiresAt <= :now
    """)
    int bulkExpirePendingBookings(@Param("now") LocalDateTime now);

    // Dohvat bookinga koji su upravo expirali (za chat poruke nakon bulk update)
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.listing l
        JOIN FETCH l.things
        JOIN FETCH b.renter
        LEFT JOIN FETCH b.conversation
        WHERE b.status = 'EXPIRED'
        AND b.expiresAt >= :since
        AND b.expiresAt <= :now
    """)
    List<Booking> findRecentlyExpired(
            @Param("since") LocalDateTime since,
            @Param("now")   LocalDateTime now);

    // ── Booking po konverzaciji ────────────────────────────────────────────
    @Query("""
        SELECT b FROM Booking b
        WHERE b.conversation.id = :conversationId
        AND b.status IN ('PENDING', 'CONFIRMED', 'ACTIVE')
        ORDER BY b.createdAt DESC
    """)
    List<Booking> findActiveBookingByConversation(@Param("conversationId") Long conversationId);
}