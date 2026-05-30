package org.example.rentathingproba.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BookingDetailsDTO {
    private Long bookingId;
    private Long listingId;
    private String listingName;
    private String listingImage;
    private LocalDate startDate;
    private LocalDate endDate;
    private long numberOfDays;
    private BigDecimal dailyRate;
    private BigDecimal deposit;
    private BigDecimal totalPrice;
    private String status;
    private String pickupPin;
    private String renterName;
    private String ownerName;
    private String myRole;
    private LocalDateTime expiresAt;
}