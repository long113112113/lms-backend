package com.example.lms_backend.exception;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                                "status", 404,
                                "error", "Not Found",
                                "message", ex.getMessage(),
                                "timestamp", Instant.now().toString()));
        }

        @ExceptionHandler(ResourceAlreadyExistsException.class)
        public ResponseEntity<Map<String, Object>> handleResourceAlreadyExists(ResourceAlreadyExistsException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                                "status", 409,
                                "error", "Conflict",
                                "message", ex.getMessage(),
                                "timestamp", Instant.now().toString()));
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                                "status", 401,
                                "error", "Unauthorized",
                                "message", ex.getMessage(),
                                "timestamp", Instant.now().toString()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
                var errors = ex.getBindingResult().getFieldErrors().stream()
                                .collect(Collectors.toMap(
                                                fieldError -> fieldError.getField(),
                                                fieldError -> fieldError.getDefaultMessage() != null
                                                                ? fieldError.getDefaultMessage()
                                                                : "Invalid value",
                                                (existing, replacement) -> existing));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                                "status", 400,
                                "error", "Bad Request",
                                "message", "Validation failed",
                                "errors", errors,
                                "timestamp", Instant.now().toString()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
                log.error("Unexpected error occurred", ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                                "status", 500,
                                "error", "Internal Server Error",
                                "message", "An unexpected error occurred",
                                "timestamp", Instant.now().toString()));
        }
}
