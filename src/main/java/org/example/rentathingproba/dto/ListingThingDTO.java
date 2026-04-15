package org.example.rentathingproba.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ListingThingDTO {
    //thing
    private String name;
    private String category;
    private String description;

    //listing
    private BigDecimal price;
    private BigDecimal securityDeposit;
    private String location;
}
