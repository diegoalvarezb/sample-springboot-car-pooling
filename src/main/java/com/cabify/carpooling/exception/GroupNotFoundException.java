package com.cabify.carpooling.exception;

/**
 * Exception thrown when a group is not found.
 */
public class GroupNotFoundException extends RuntimeException {

    /**
     * Creates an exception with default message.
     */
    public GroupNotFoundException() {
        super("Group not found");
    }

    /**
     * Creates an exception with a custom message.
     *
     * @param message The error message describing which group was not found
     */
    public GroupNotFoundException(String message) {
        super(message);
    }
}
