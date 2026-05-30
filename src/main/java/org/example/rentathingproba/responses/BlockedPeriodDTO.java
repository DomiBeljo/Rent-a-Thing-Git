package org.example.rentathingproba.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class BlockedPeriodDTO {
    private LocalDate startDate;
    private LocalDate endDate;
}
