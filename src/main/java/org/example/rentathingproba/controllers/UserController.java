package org.example.rentathingproba.controllers;

import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.responses.ListingResponseDTO;
import org.example.rentathingproba.responses.UserResponseDTO;
import org.example.rentathingproba.service.application.BookingService;
import org.example.rentathingproba.service.application.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping("/users")
@RestController
public class UserController {
    private final UserService userService;
    private final BookingService bookingService;

    public UserController(UserService userService, BookingService bookingService) {
        this.userService = userService;
        this.bookingService = bookingService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> authenticatedUser(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getProfile(currentUser));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> allUsers() {
        return ResponseEntity.ok(userService.findAllUsers());
    }

    // ── Favourites ────────────────────────────────────────────────────────────

    @GetMapping("/me/favourites")
    public ResponseEntity<List<ListingResponseDTO>> getFavourites(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getFavourites(currentUser));
    }

    @PostMapping("/me/favourites/{listingId}")
    public ResponseEntity<Void> addFavourite(@AuthenticationPrincipal User currentUser,
                                             @PathVariable Long listingId) {
        userService.addFavourite(currentUser, listingId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/favourites/{listingId}")
    public ResponseEntity<Void> removeFavourite(@AuthenticationPrincipal User currentUser,
                                                @PathVariable Long listingId) {
        userService.removeFavourite(currentUser, listingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/favourites/{listingId}/check")
    public ResponseEntity<Map<String, Boolean>> checkFavourite(@AuthenticationPrincipal User currentUser,
                                                               @PathVariable Long listingId) {
        boolean isFav = userService.isFavourite(currentUser, listingId);
        return ResponseEntity.ok(Map.of("isFavourite", isFav));
    }

    // ── Rating ────────────────────────────────────────────────────────────────

    /**
     * ✅ FIX 3: Rating endpoint sada zahtijeva bookingId.
     * Service provjerava: booking je COMPLETED, caller je sudionik,
     * booking.reviewed == false, score je 1-5, ne možeš ratati sam sebe.
     *
     * Request body: { "bookingId": 42, "score": 4.5 }
     */
    @PostMapping("/{userId}/rate")
    public ResponseEntity<Void> rateUser(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {

        Long bookingId = Long.valueOf(body.get("bookingId").toString());
        double score   = ((Number) body.get("score")).doubleValue();

        bookingService.rateUserFromBooking(currentUser, userId, bookingId, score);
        userService.rateUser(userId, score);

        return ResponseEntity.noContent().build();
    }
}