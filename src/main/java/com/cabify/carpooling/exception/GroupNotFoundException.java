package com.cabify.carpooling.exception;

/**
 * Exception thrown when a group is not found.
 *
 * <p>This exception is handled by {@link com.cabify.carpooling.controller.GlobalExceptionHandler}
 * and returns HTTP 404 Not Found.
 *
 * <p>This typically occurs when:
 * <ul>
 *   <li>Attempting to locate a group that doesn't exist</li>
 *   <li>Attempting to drop off a group that was never registered</li>
 *   <li>Using an invalid group ID</li>
 * </ul>
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
