package com.tripplanner.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception for validation failures with user-friendly messages
 */
public class ValidationException extends RuntimeException {
    
    private final List<String> validationErrors;
    private final String userMessage;
    
    public ValidationException(String message) {
        super(message);
        this.validationErrors = new ArrayList<>();
        this.validationErrors.add(message);
        this.userMessage = message;
    }
    
    public ValidationException(String message, String userMessage) {
        super(message);
        this.validationErrors = new ArrayList<>();
        this.validationErrors.add(message);
        this.userMessage = userMessage;
    }
    
    public ValidationException(List<String> validationErrors) {
        super("Validation failed: " + String.join(", ", validationErrors));
        this.validationErrors = new ArrayList<>(validationErrors);
        this.userMessage = "Validation failed. Please check your input.";
    }
    
    public ValidationException(List<String> validationErrors, String userMessage) {
        super("Validation failed: " + String.join(", ", validationErrors));
        this.validationErrors = new ArrayList<>(validationErrors);
        this.userMessage = userMessage;
    }
    
    public List<String> getValidationErrors() {
        return validationErrors;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
}
