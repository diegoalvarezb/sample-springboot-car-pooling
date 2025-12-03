package com.cabify.carpooling.exception;

/**
 * Exception thrown when attempting to use a duplicated ID.
 *
 * <p>This exception is currently not handled by any exception handler.
 * If you want to use it, add a handler in {@link com.cabify.carpooling.controller.GlobalExceptionHandler}.
 *
 * <p>This would typically occur when:
 * <ul>
 *   <li>Loading cars with duplicate IDs</li>
 *   <li>Registering groups with duplicate IDs</li>
 * </ul>
 *
 * <p><strong>Note:</strong> This exception is currently unused in the codebase.
 * Consider removing it if not needed, or add a handler if you plan to use it.
 */
public class DuplicatedIdException extends RuntimeException {

    /**
     * Creates an exception with default message.
     */
    public DuplicatedIdException() {
        super("Duplicated ID");
    }

    /**
     * Creates an exception with a custom message.
     *
     * @param message The error message describing which ID is duplicated
     */
    public DuplicatedIdException(String message) {
        super(message);
    }
}
