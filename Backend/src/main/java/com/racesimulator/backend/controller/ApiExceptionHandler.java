package com.racesimulator.backend.controller;

import com.racesimulator.backend.dto.ApiErrorResponse;
import com.racesimulator.backend.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> invalidBody(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "One or more request fields are invalid", fields, false);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> invalidParameter(ConstraintViolationException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                fields.put(violation.getPropertyPath().toString(), violation.getMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "One or more request parameters are invalid", fields, false);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> unreadable(HttpMessageNotReadableException exception) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "The request body is missing or contains an unsupported value", Map.of(), false);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), Map.of(), false);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> invalidConfiguration(IllegalArgumentException exception) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_RACE_CONFIGURATION",
                exception.getMessage(), Map.of(), false);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> simulationFailure(IllegalStateException exception) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "SIMULATION_DID_NOT_FINISH",
                exception.getMessage(), Map.of(), false);
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message,
                                                       Map<String, String> fields, boolean retryable) {
        String correlationId = UUID.randomUUID().toString();
        var body = new ApiErrorResponse(Instant.now(), status.value(), code, message,
                Map.copyOf(fields), correlationId, retryable);
        return ResponseEntity.status(status)
                .header("X-Correlation-Id", correlationId)
                .body(body);
    }
}
