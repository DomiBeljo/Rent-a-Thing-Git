package org.example.rentathingproba.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ThingDTO {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "category is required")
    private String category;

    @NotBlank(message = "description is required")
    private String description;

    @NotNull(message = "imageUrls is required")
    private List<String> imageUrls;
}