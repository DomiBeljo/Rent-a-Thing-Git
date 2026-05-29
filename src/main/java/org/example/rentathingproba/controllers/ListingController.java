package org.example.rentathingproba.controllers;

import org.example.rentathingproba.dto.ListingDTO;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.responses.ListingResponseDTO;
import org.example.rentathingproba.service.application.ListingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/listings")
public class ListingController {
    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping
    public ResponseEntity<ListingResponseDTO> create(@RequestBody ListingDTO dto, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(listingService.createListing(dto, currentUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListingResponseDTO> update(@PathVariable Long id, @RequestBody ListingDTO dto, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(listingService.updateListing(id, dto, currentUser));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleAvailability(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        listingService.isItAvailable(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ListingResponseDTO>> search(@RequestParam String query) {
        return ResponseEntity.ok(listingService.searchListing(query));
    }

    @GetMapping("/recommended")
    public ResponseEntity<List<ListingResponseDTO>> recommended() {
        return ResponseEntity.ok(listingService.getRecommended());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.getListingById(id));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ListingResponseDTO>> myListings(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(listingService.getUserListing(currentUser.getId()));
    }
}