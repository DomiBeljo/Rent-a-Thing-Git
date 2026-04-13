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
    private Long id;
    private BigDecimal dailyRentPrice;
    private String location;
    private Boolean isAvailable;
    private String imageUrls;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
}


