package com.cabify.carpooling.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when the request payload is invalid.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid payload")
public class InvalidPayloadException extends RuntimeException {
    public InvalidPayloadException() {
        super("Invalid payload");
    }

    public InvalidPayloadException(String message) {
        super(message);
    }
}
