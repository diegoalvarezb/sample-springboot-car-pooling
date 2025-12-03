package com.cabify.carpooling.exception;

/**
 * Exception thrown when the request payload is invalid.
 *
 * <p>This exception is handled by {@link com.cabify.carpooling.controller.GlobalExceptionHandler}
 * and returns HTTP 400 Bad Request.
 *
 * <p>Common scenarios:
 * <ul>
 *   <li>Missing required fields</li>
 *   <li>Invalid data format</li>
 *   <li>Values outside allowed range</li>
 *   <li>Null or empty collections</li>
 * </ul>
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
