// ─────────────────────────────────────────────
// FILE 1: CreateBookingDTO.java
// ─────────────────────────────────────────────
package org.example.rentathingproba.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class CreateBookingDTO {

    @NotNull(message = "listingId is required")
    private Long listingId;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    private LocalDate endDate;

    private Long conversationId;
}