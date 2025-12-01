package com.cabify.carpooling.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to register a group that already exists.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Group already exists")
public class ExistingGroupException extends RuntimeException {
    public ExistingGroupException() {
        super("Group already exists");
    }

    public ExistingGroupException(String message) {
        super(message);
    }
}
