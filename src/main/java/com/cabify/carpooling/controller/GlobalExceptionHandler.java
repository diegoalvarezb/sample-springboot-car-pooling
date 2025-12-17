package com.cabify.carpooling.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.cabify.carpooling.exception.ExistingGroupException;
import com.cabify.carpooling.exception.GroupNotFoundException;
import com.cabify.carpooling.exception.InvalidPayloadException;

/**
 * Global exception handler to catch and log errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles invalid payload exceptions.
     */
    @ExceptionHandler(InvalidPayloadException.class)
    public ResponseEntity<Void> handleInvalidPayload(InvalidPayloadException e) {
        log.error("Invalid payload: {}", e.getMessage());

        return ResponseEntity.badRequest().build();
    }

    /**
     * Handles existing group exceptions.
     */
    @ExceptionHandler(ExistingGroupException.class)
    public ResponseEntity<Void> handleExistingGroup(ExistingGroupException e) {
        log.error("Group already exists: {}", e.getMessage());

        return ResponseEntity.badRequest().build();
    }

    /**
     * Handles group not found exceptions.
     */
    @ExceptionHandler(GroupNotFoundException.class)
    public ResponseEntity<Void> handleGroupNotFound(GroupNotFoundException e) {
        log.error("Group not found: {}", e.getMessage());

        return ResponseEntity.notFound().build();
    }

    /**
     * Handles HTTP message not readable exceptions.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.error("Failed to read HTTP message. This might be due to: " +
                "1) Request body too large, 2) Invalid JSON format, 3) Deserialization error", e);

        Throwable rootCause = e.getRootCause();
        String rootCauseMessage = rootCause != null ? rootCause.getMessage() : e.getMessage();

        log.error("Root cause: {}", rootCauseMessage);

        return ResponseEntity.badRequest().build();
    }

    /**
     * Handles max upload size exceeded exceptions.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.error("Request body size exceeded maximum allowed size. Max size: {}, Actual size: {}",
                e.getMaxUploadSize(), "unknown");

        return ResponseEntity.badRequest().build();
    }

    /**
     * Handles generic exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleGenericException(Exception e) {
        log.error("Unexpected error in controller", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
