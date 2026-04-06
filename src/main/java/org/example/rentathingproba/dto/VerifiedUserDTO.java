package org.example.rentathingproba.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifiedUserDTO {
    private String email;
    private String verificationCode;
}
