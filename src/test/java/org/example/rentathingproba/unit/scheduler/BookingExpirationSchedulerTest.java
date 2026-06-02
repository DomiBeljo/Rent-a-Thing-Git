package org.example.rentathingproba.unit.scheduler;

import org.example.rentathingproba.scheduler.BookingExpirationScheduler;
import org.example.rentathingproba.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingExpirationScheduler Unit Tests")
class BookingExpirationSchedulerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingExpirationScheduler scheduler;

    @Test
    @DisplayName("expirePendingBookings: delegates to BookingService and completes without error")
    void expirePendingBookings_delegatesToService() {
        when(bookingService.expirePendingBookings()).thenReturn(3);

        scheduler.expirePendingBookings();

        verify(bookingService, times(1)).expirePendingBookings();
    }

    @Test
    @DisplayName("expirePendingBookings: handles service exceptions gracefully without rethrowing")
    void expirePendingBookings_handlesExceptionGracefully() {
        when(bookingService.expirePendingBookings()).thenThrow(new RuntimeException("DB error"));

        scheduler.expirePendingBookings();

        verify(bookingService, times(1)).expirePendingBookings();
    }

    @Test
    @DisplayName("expirePendingBookings: does not fail when service returns zero expired bookings")
    void expirePendingBookings_zeroExpiredBookings() {
        when(bookingService.expirePendingBookings()).thenReturn(0);

        scheduler.expirePendingBookings();

        verify(bookingService, times(1)).expirePendingBookings();
    }
}