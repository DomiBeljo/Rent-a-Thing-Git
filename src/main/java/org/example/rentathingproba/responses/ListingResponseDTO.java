package org.example.rentathingproba.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor //da se ne otkrije hash od usera
public class ListingResponseDTO {
    private Long listingId;
    private BigDecimal price;
    private String location;
    private boolean isAvailable;
    private LocalDateTime createdAt;
    private BigDecimal securityDeposit;

    private long thingId;
    private String name;
    private String category;
    private String description;
    private String imageUrls;

    private Long userId;
    private String userName;
}


