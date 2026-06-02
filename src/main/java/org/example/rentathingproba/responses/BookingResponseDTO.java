package org.example.rentathingproba.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.rentathingproba.entities.Booking;
import org.example.rentathingproba.entities.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BookingResponseDTO {
    private Long bookingId;
    private Long listingId;
    private String listingName;
    private Long renterId;
    private String renterName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingStatus status;
    private BigDecimal pricePerDay;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String pickupPin;
}