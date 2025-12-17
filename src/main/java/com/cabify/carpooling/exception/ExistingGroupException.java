package com.cabify.carpooling.exception;

/**
 * Exception thrown when attempting to register a group that already exists.
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
