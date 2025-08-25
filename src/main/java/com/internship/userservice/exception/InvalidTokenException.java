package com.internship.userservice.exception;

public class InvalidTokenException extends RuntimeException{

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
