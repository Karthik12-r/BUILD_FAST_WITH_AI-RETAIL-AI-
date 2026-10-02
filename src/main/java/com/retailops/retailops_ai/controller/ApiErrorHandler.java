package com.retailops.retailops_ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException exception) {
        String message = exception.getReason() == null ? "Request could not be completed" : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode()).body(new ApiError(message));
    }

    public record ApiError(String message) {
    }
}