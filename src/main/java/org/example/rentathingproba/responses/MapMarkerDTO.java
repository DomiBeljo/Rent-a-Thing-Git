package org.example.rentathingproba.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;


@Getter
@AllArgsConstructor
public class MapMarkerDTO {

    private Long listingId;

    private String location;

    private String name;

    private String category;

    private BigDecimal price;

    private String thumbnailUrl;

    private boolean isAvailable;

    private Long userId;

    private String userName;

    private double userRating;
}
