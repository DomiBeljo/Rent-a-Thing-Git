package org.example.rentathingproba.controllers;

import org.example.rentathingproba.dto.CreateBookingDTO;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.responses.BlockedPeriodDTO;
import org.example.rentathingproba.responses.BookingResponseDTO;
import org.example.rentathingproba.service.application.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponseDTO> create(
            @RequestBody CreateBookingDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(bookingService.createBooking(dto, currentUser));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponseDTO>> myBookings(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(bookingService.getMyBookings(currentUser));
    }

    @GetMapping("/listing/{listingId}")
    public ResponseEntity<List<BookingResponseDTO>> forListing(
            @PathVariable Long listingId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(bookingService.getBookingsForListing(listingId, currentUser));
    }

    @GetMapping("/listing/{listingId}/blocked")
    public ResponseEntity<List<BlockedPeriodDTO>> blockedPeriods(
            @PathVariable Long listingId) {
        return ResponseEntity.ok(bookingService.getBlockedPeriods(listingId));
    }

    @GetMapping("/listing/{listingId}/available")
    public ResponseEntity<java.util.Map<String, Boolean>> checkAvailability(
            @PathVariable Long listingId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        boolean available = bookingService.isAvailable(listingId, startDate, endDate);
        return ResponseEntity.ok(java.util.Map.of("available", available));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<BookingResponseDTO> confirm(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(bookingService.confirmBooking(id, currentUser));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDTO> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, currentUser));
    }
}
