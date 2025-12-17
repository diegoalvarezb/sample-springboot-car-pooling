package com.cabify.carpooling.exception;

/**
 * Exception thrown when the request payload is invalid.
 */
public class InvalidPayloadException extends RuntimeException {

    /**
     * Creates an exception with default message.
     */
    public InvalidPayloadException() {
        super("Invalid payload");
    }

    /**
     * Creates an exception with a custom message.
     *
     * @param message The error message describing the validation failure
     */
    public InvalidPayloadException(String message) {
        super(message);
    }
}
