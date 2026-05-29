package org.example.rentathingproba.controllers;

import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.responses.ListingResponseDTO;
import org.example.rentathingproba.responses.UserResponseDTO;
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

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> authenticatedUser(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getProfile(currentUser));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> allUsers() {
        return ResponseEntity.ok(userService.findAllUsers());
    }

    //Favourites

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

    // Rating

    @PostMapping("/{userId}/rate")
    public ResponseEntity<Void> rateUser(@PathVariable Long userId,
                                         @RequestBody Map<String, Double> body) {
        double score = body.getOrDefault("score", 0.0);
        if (score < 1.0 || score > 5.0) return ResponseEntity.badRequest().build();
        userService.rateUser(userId, score);
        return ResponseEntity.noContent().build();
    }
}
