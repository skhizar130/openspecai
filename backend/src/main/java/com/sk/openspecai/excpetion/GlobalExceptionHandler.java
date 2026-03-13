package com.sk.openspecai.excpetion;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IdGenerationException.class)
    public ResponseEntity<ApiError> handle(IdGenerationException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(SpecReadException.class)
    public ResponseEntity<ApiError> handle(SpecReadException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(SpecWriteException.class)
    public ResponseEntity<ApiError> handle(SpecWriteException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(SpecParsingException.class)
    public ResponseEntity<ApiError> handle(SpecParsingException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(SpecNotFoundException.class)
    public ResponseEntity<ApiError> handle(SpecNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(OwnerNotFoundException.class)
    public ResponseEntity<ApiError> handle(OwnerNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(InvalidSwaggerHubTokenException.class)
    public ResponseEntity<ApiError> handle(InvalidSwaggerHubTokenException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(SwaggerHubUnavailableException.class)
    public ResponseEntity<ApiError> handle(SwaggerHubUnavailableException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(SwaggerHubPublishException.class)
    public ResponseEntity<ApiError> handle(SwaggerHubPublishException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(SpecValidationException.class)
    public ResponseEntity<ApiError> handle(SpecValidationException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorMessage> handle(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorMessage("Validation failed", message, Instant.now()));
    }

    @ExceptionHandler(EndpointNotFoundException.class)
    public ResponseEntity<ApiError> handle(EndpointNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(e.getMessage(), Instant.now()));
    }
}
