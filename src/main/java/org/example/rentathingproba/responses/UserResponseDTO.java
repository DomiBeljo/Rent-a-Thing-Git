package org.example.rentathingproba.responses;

import lombok.Getter;

@Getter
public class UserResponseDTO {
    private final Long id;
    private final String username;
    private final String email;
    private final double rating;
    private final int ratingCount;
    private final int favouriteCount;
    private final int listingCount;

    public UserResponseDTO(Long id, String username, String email,
                           double rating, int ratingCount, int favouriteCount, int listingCount) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.favouriteCount = favouriteCount;
        this.listingCount = listingCount;
    }
}
