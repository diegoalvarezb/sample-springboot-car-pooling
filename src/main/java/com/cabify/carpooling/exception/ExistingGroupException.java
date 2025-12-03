package com.cabify.carpooling.exception;

/**
 * Exception thrown when attempting to register a group that already exists.
 *
 * <p>This exception is handled by {@link com.cabify.carpooling.controller.GlobalExceptionHandler}
 * and returns HTTP 400 Bad Request.
 *
 * <p>This typically occurs when:
 * <ul>
 *   <li>A group with the same ID is already registered</li>
 *   <li>Attempting to create a duplicate journey request</li>
 * </ul>
 */
public class ExistingGroupException extends RuntimeException {

    /**
     * Creates an exception with default message.
     */
    public ExistingGroupException() {
        super("Group already exists");
    }

    /**
     * Creates an exception with a custom message.
     *
     * @param message The error message describing why the group already exists
     */
    public ExistingGroupException(String message) {
        super(message);
    }
}
