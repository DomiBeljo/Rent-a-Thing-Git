package org.example.rentathingproba.responses;

import lombok.Getter;

//Doesnt expose passwords
@Getter
public class UserResponseDTO {
    private final Long id;
    private final String username;
    private final String email;

    public UserResponseDTO(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

}
