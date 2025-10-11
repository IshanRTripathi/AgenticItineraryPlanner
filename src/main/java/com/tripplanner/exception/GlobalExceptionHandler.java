package com.tripplanner.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

/**
 * Global exception handler for consistent error responses across the application.
 * Provides structured error handling for AI service failures, ownership errors, and other exceptions.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle AI service failures
     */
    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ErrorResponse> handleAiServiceException(AiServiceException ex, WebRequest request) {
        logger.error("=== AI SERVICE EXCEPTION ===");
        logger.error("Provider: {}", ex.getProvider());
        logger.error("Operation: {}", ex.getOperation());
        logger.error("Error: {}", ex.getMessage(), ex);
        logger.error("Request: {}", request.getDescription(false));
        logger.error("============================");
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("AI Service Error")
                .message("AI service is temporarily unavailable. Please try again in a few moments.")
                .path(request.getDescription(false).replace("uri=", ""))
                .details(ErrorDetails.builder()
                        .provider(ex.getProvider())
                        .operation(ex.getOperation())
                        .retryable(true)
                        .suggestedAction("Wait a few seconds and try again")
                        .build())
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    /**
     * Handle ownership validation errors
     */
    @ExceptionHandler(OwnershipException.class)
    public ResponseEntity<ErrorResponse> handleOwnershipException(OwnershipException ex, WebRequest request) {
        logger.error("=== OWNERSHIP EXCEPTION ===");
        logger.error("User ID: {}", ex.getUserId());
        logger.error("Itinerary ID: {}", ex.getItineraryId());
        logger.error("Operation: {}", ex.getOperation());
        logger.error("Error: {}", ex.getMessage(), ex);
        logger.error("Request: {}", request.getDescription(false));
        logger.error("===========================");
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message("You don't have permission to access this itinerary.")
                .path(request.getDescription(false).replace("uri=", ""))
                .details(ErrorDetails.builder()
                        .userId(ex.getUserId())
                        .itineraryId(ex.getItineraryId())
                        .operation(ex.getOperation())
                        .retryable(false)
                        .suggestedAction("Make sure you have access to this itinerary")
                        .build())
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
    
    /**
     * Handle itinerary not found errors
     */
    @ExceptionHandler(ItineraryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleItineraryNotFoundException(ItineraryNotFoundException ex, WebRequest request) {
        logger.warn("=== ITINERARY NOT FOUND ===");
        logger.warn("Itinerary ID: {}", ex.getItineraryId());
        logger.warn("User ID: {}", ex.getUserId());
        logger.warn("Error: {}", ex.getMessage());
        logger.warn("Request: {}", request.getDescription(false));
        logger.warn("===========================");
        
        // Check if this might be a generation-in-progress scenario
        boolean isGenerationInProgress = ex.getMessage() != null && 
                (ex.getMessage().toLowerCase().contains("generation") || 
                 ex.getMessage().toLowerCase().contains("creating") ||
                 ex.getMessage().toLowerCase().contains("being generated"));
        
        String userMessage = isGenerationInProgress 
                ? "Your itinerary is still being generated. Please wait a moment and try again."
                : "Itinerary not found. It may have been deleted or you may not have access to it.";
        
        String suggestedAction = isGenerationInProgress
                ? "Wait 30-60 seconds and refresh the page"
                : "Check the URL or try creating a new itinerary";
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Itinerary Not Found")
                .message(userMessage)
                .path(request.getDescription(false).replace("uri=", ""))
                .details(ErrorDetails.builder()
                        .itineraryId(ex.getItineraryId())
                        .userId(ex.getUserId())
                        .retryable(isGenerationInProgress)
                        .suggestedAction(suggestedAction)
                        .generationInProgress(isGenerationInProgress)
                        .build())
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handle validation errors
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex, WebRequest request) {
        logger.warn("=== VALIDATION EXCEPTION ===");
        logger.warn("Field: {}", ex.getField());
        logger.warn("Value: {}", ex.getValue());
        logger.warn("Error: {}", ex.getMessage());
        logger.warn("Request: {}", request.getDescription(false));
        logger.warn("============================");
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message("The provided information is invalid. Please check your input and try again.")
                .path(request.getDescription(false).replace("uri=", ""))
                .details(ErrorDetails.builder()
                        .field(ex.getField())
                        .value(ex.getValue())
                        .retryable(false)
                        .suggestedAction("Check your input and correct any errors")
                        .build())
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle general runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
        logger.error("=== RUNTIME EXCEPTION ===");
        logger.error("Type: {}", ex.getClass().getSimpleName());
        logger.error("Error: {}", ex.getMessage(), ex);
        logger.error("Request: {}", request.getDescription(false));
        logger.error("=========================");
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again or contact support if the problem persists.")
                .path(request.getDescription(false).replace("uri=", ""))
                .details(ErrorDetails.builder()
                        .errorType(ex.getClass().getSimpleName())
                        .retryable(true)
                        .suggestedAction("Try again in a few minutes or contact support")
                        .build())
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        logger.error("=== GENERIC EXCEPTION ===");
        logger.error("Type: {}", ex.getClass().getSimpleName());
        logger.error("Error: {}", ex.getMessage(), ex);
        logger.error("Request: {}", request.getDescription(false));
        logger.error("=========================");
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Server Error")
                .message("A server error occurred. Please try again later.")
                .path(request.getDescription(false).replace("uri=", ""))
                .details(ErrorDetails.builder()
                        .errorType(ex.getClass().getSimpleName())
                        .retryable(true)
                        .suggestedAction("Try again later or contact support")
                        .build())
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}