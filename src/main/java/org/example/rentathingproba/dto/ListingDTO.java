package org.example.rentathingproba.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ListingDTO {
    public BigDecimal dailyRentPrice;
    public String location;
}
