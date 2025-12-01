package com.cabify.carpooling.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a group is not found.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Group not found")
public class GroupNotFoundException extends RuntimeException {
    public GroupNotFoundException() {
        super("Group not found");
    }

    public GroupNotFoundException(String message) {
        super(message);
    }
}
