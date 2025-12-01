package com.cabify.carpooling.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to use a duplicated ID.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Duplicated ID")
public class DuplicatedIdException extends RuntimeException {
    public DuplicatedIdException() {
        super("Duplicated ID");
    }

    public DuplicatedIdException(String message) {
        super(message);
    }
}
