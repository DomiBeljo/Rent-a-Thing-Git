package org.example.rentathingproba.responses;

import lombok.Getter;

//For endpoints that return a plain success confirmation
//Can read response.data.message
@Getter
public class MessageResponse {

    private final String message;

    public  MessageResponse(String message) {
        this.message = message;
    }

}
