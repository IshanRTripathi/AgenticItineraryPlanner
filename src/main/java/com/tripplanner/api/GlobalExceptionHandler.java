package com.tripplanner.api;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        logger.error("=== VALIDATION ERROR ===");
        logger.error("Request: {}", request.getDescription(false));
        logger.error("Error: {}", ex.getMessage());
        logger.error("Field Errors:");
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
            logger.error("  {}: {}", fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                "Invalid request data",
                "Please check the provided data and try again",
                Instant.now(),
                request.getDescription(false),
                fieldErrors
        );
        
        logger.error("=== VALIDATION ERROR RESPONSE ===");
        logger.error("Status: 400 BAD REQUEST");
        logger.error("Error Response: {}", errorResponse);
        logger.error("===============================");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle constraint violation errors.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        logger.warn("Constraint violation: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                "CONSTRAINT_VIOLATION",
                "Constraint violation",
                ex.getMessage(),
                Instant.now(),
                request.getDescription(false),
                null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle access denied errors.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        logger.warn("Access denied: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                "ACCESS_DENIED",
                "Access denied",
                "You don't have permission to access this resource",
                Instant.now(),
                request.getDescription(false),
                null
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handle illegal argument errors.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        logger.warn("Illegal argument: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                "INVALID_ARGUMENT",
                "Invalid argument",
                ex.getMessage(),
                Instant.now(),
                request.getDescription(false),
                null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle unsupported operation errors.
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedOperationException(
            UnsupportedOperationException ex, WebRequest request) {
        
        logger.warn("Unsupported operation: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                "NOT_IMPLEMENTED",
                "Feature not implemented",
                ex.getMessage(),
                Instant.now(),
                request.getDescription(false),
                null
        );
        
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse);
    }
    
    /**
     * Handle runtime errors.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        logger.error("Runtime error: {}", ex.getMessage(), ex);
        
        // Check for specific runtime exceptions
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("not found")) {
                ErrorResponse errorResponse = new ErrorResponse(
                        "NOT_FOUND",
                        "Resource not found",
                        ex.getMessage(),
                        Instant.now(),
                        request.getDescription(false),
                        null
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            if (ex.getMessage().contains("Access denied")) {
                ErrorResponse errorResponse = new ErrorResponse(
                        "ACCESS_DENIED",
                        "Access denied",
                        ex.getMessage(),
                        Instant.now(),
                        request.getDescription(false),
                        null
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_ERROR",
                "Internal server error",
                "An unexpected error occurred",
                Instant.now(),
                request.getDescription(false),
                null
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Handle IO exceptions (including SSE disconnects).
     */
    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(
            java.io.IOException ex, WebRequest request) {
        
        String description = request.getDescription(false);
        
        // If this is an SSE (text/event-stream) request, silently handle client disconnects
        if (description != null && description.contains("/agents/stream")) {
            logger.debug("SSE client disconnected: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        
        // For non-SSE IO exceptions, log as warning
        logger.warn("IO error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                "IO_ERROR",
                "IO error occurred",
                ex.getMessage(),
                Instant.now(),
                description,
                null
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        
        // If this is an SSE (text/event-stream) request, return 204 to avoid JSON write with wrong content-type
        String description = request.getDescription(false);
        if (description != null && description.contains("/agents/stream")) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_ERROR",
                "Internal server error",
                "An unexpected error occurred",
                Instant.now(),
                description,
                null
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Error response DTO.
     */
    public record ErrorResponse(
            String code,
            String message,
            String hint,
            Instant timestamp,
            String path,
            Map<String, String> details
    ) {}
}

