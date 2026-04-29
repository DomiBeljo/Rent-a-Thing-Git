package org.example.rentathingproba.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String token;
    private Long expiresIn;

    //So the app knows whos logged in
    private long userId;
    private String username;
    private String email;

    public LoginResponse(String token, Long expiresIn, long userId, String username, String email) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.username = username;
        this.email = email;
    }
}
