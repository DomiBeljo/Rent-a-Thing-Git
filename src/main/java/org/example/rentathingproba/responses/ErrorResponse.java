package org.example.rentathingproba.responses;

import lombok.Getter;
import org.example.rentathingproba.exceptions.ErrorCode;

import java.time.LocalDateTime;

//Error response returned to GlobalExceptionHandler
@Getter
public class ErrorResponse {
    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final ErrorCode code;
    private final String message;

    public ErrorResponse(int status, String error, ErrorCode code, String message) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
    }

}
