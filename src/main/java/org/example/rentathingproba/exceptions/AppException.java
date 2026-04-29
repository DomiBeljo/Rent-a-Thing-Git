package org.example.rentathingproba.exceptions;

import lombok.Getter;

//Class for every application exceptions.
@Getter
public abstract class AppException extends RuntimeException {
    private final ErrorCode errorCode;

    protected AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }


}
