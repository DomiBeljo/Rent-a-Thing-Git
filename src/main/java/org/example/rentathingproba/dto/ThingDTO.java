package org.example.rentathingproba.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ThingDTO {
    private String name;
    private String category;
    private String description;
    private String imageUrls;
}
