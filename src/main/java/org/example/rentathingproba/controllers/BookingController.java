package org.example.rentathingproba.controllers;

import org.example.rentathingproba.dto.CreateBookingDTO;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.responses.BlockedPeriodDTO;
import org.example.rentathingproba.responses.BookingResponseDTO;
import org.example.rentathingproba.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/request")
    public ResponseEntity<BookingResponseDTO> createRequest(
            @Valid @RequestBody CreateBookingDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(bookingService.createBookingRequest(dto, currentUser));
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
    public ResponseEntity<Map<String, Boolean>> checkAvailability(
            @PathVariable Long listingId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        boolean available = bookingService.isAvailable(listingId, startDate, endDate);
        return ResponseEntity.ok(Map.of("available", available));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingResponseDTO> confirm(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(bookingService.confirmBooking(id, currentUser));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<BookingResponseDTO> decline(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(bookingService.declineBooking(id, currentUser));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDTO> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, currentUser));
    }

    @PostMapping("/{id}/pickup")
    public ResponseEntity<BookingResponseDTO> confirmPickup(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        String pin = body.get("pin");
        return ResponseEntity.ok(bookingService.confirmPickup(id, pin, currentUser));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<BookingResponseDTO> confirmReturn(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(bookingService.confirmReturn(id, currentUser));
    }

    @GetMapping("/{id}/pin")
    public ResponseEntity<Map<String, String>> getPin(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        String pin = bookingService.getPickupPin(id, currentUser);
        return ResponseEntity.ok(Map.of("pin", pin));
    }
}