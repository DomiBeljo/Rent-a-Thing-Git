package org.example.rentathingproba.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class ListingDTO {

    @NotNull(message = "thingId is required")
    private Long thingId;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.01", message = "price must be greater than 0")
    private BigDecimal price;

    private BigDecimal securityDeposit;

    @NotBlank(message = "location is required")
    private String location;

    private Double latitude;
    private Double longitude;
}