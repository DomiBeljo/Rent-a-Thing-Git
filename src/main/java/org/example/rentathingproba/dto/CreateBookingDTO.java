package org.example.rentathingproba.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class CreateBookingDTO {
    private Long listingId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long conversationId;
}