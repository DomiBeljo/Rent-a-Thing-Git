package org.example.rentathingproba.repository;

import org.example.rentathingproba.entities.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByListingIdOrderByStartDateAsc(Long listingId);
    List<Booking> findByRenterIdOrderByStartDateDesc(Long renterId);

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

    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.listing.id = :listingId
        AND b.status IN ('CONFIRMED', 'PENDING', 'ACTIVE')
        AND b.startDate <= :endDate
        AND b.endDate   >= :startDate
    """)
    boolean existsOverlappingBooking(@Param("listingId") Long listingId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate")   LocalDate endDate);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.listing.id = :listingId
        AND b.status IN ('CONFIRMED', 'PENDING', 'ACTIVE')
        AND b.endDate >= :from
        ORDER BY b.startDate ASC
    """)
    List<Booking> findActiveBookingsFrom(@Param("listingId") Long listingId,
                                         @Param("from") LocalDate from);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = 'PENDING'
        AND b.expiresAt <= :now
    """)
    List<Booking> findExpiredPendingBookings(@Param("now") LocalDateTime now);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.conversation.id = :conversationId
        AND b.status IN ('PENDING', 'CONFIRMED', 'ACTIVE')
        ORDER BY b.createdAt DESC
    """)
    List<Booking> findActiveBookingByConversation(@Param("conversationId") Long conversationId);
}