package org.example.rentathingproba.repository;

import org.example.rentathingproba.entities.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByListingIdOrderByStartDateAsc(Long listingId);

    List<Booking> findByRenterIdOrderByStartDateDesc(Long renterId);

    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.listing.id = :listingId
          AND b.status IN ('CONFIRMED', 'PENDING')
          AND b.startDate <= :endDate
          AND b.endDate   >= :startDate
    """)
    boolean existsOverlappingBooking(@Param("listingId") Long listingId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate")   LocalDate endDate);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.listing.id = :listingId
          AND b.status IN ('CONFIRMED', 'PENDING')
          AND b.endDate >= :from
        ORDER BY b.startDate ASC
    """)
    List<Booking> findActiveBookingsFrom(@Param("listingId") Long listingId,
                                         @Param("from") LocalDate from);
}
