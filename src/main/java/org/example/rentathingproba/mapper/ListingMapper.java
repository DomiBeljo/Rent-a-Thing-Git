package org.example.rentathingproba.mapper;

import org.example.rentathingproba.dto.ListingDTO;
import org.example.rentathingproba.entities.Listing;
import org.example.rentathingproba.entities.Thing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.responses.ListingResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class ListingMapper {
    public Listing toEntity(ListingDTO dto, User owner, Thing thing) {
        Listing listing = new Listing();
        listing.setThings(thing);
        listing.setUser(owner);
        listing.setPrice(dto.getPrice());
        listing.setSecurityDeposit(dto.getSecurityDeposit());
        listing.setLocation(dto.getLocation());
        listing.setIsAvailable(true);
        listing.setImageUrls(thing.getImageUrls() != null ? thing.getImageUrls() : "");
        return listing;
    }

    public void updateEntity(Listing listing, ListingDTO dto) {
        listing.setPrice(dto.getPrice());
        listing.setSecurityDeposit(dto.getSecurityDeposit());
        listing.setLocation(dto.getLocation());
    }

    public ListingResponseDTO toResponse(Listing l) {
        return new ListingResponseDTO(
                l.getId(),
                l.getPrice(),
                l.getLocation(),
                l.getIsAvailable(),
                l.getCreatedAt(),
                l.getSecurityDeposit(),
                l.getThings().getId(),
                l.getThings().getName(),
                l.getThings().getCategory(),
                l.getThings().getDescription(),
                l.getImageUrls() != null ? l.getImageUrls() : "",
                l.getUser().getId(),
                l.getUser().getUsername()
        );
    }
}
