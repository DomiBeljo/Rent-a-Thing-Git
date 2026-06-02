package org.example.rentathingproba.scheduler;

import org.example.rentathingproba.service.application.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingExpirationScheduler {
    private static final Logger log = LoggerFactory.getLogger(BookingExpirationScheduler.class);

    private final BookingService bookingService;

    public BookingExpirationScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedRate = 300000)
    public void expirePendingBookings() {
        try {
            int expired = bookingService.expirePendingBookings();
            if (expired > 0) {
                log.info("Scheduler: {} booking(s) auto-expired", expired);
            }
        } catch (Exception e) {
            log.error("Scheduler error while expiring bookings: {}", e.getMessage(), e);
        }
    }
}