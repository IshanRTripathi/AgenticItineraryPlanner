package com.tripplanner.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * LLMSchemaValidator - Validates LLM JSON responses against schemas.
 * 
 * CRITICAL: Prevents malformed LLM responses from corrupting data.
 * 
 * Features:
 * 1. JSON schema validation
 * 2. Detailed error reporting
 * 3. Retry recommendations
 * 4. Schema caching for performance
 */
@Service
public class LLMSchemaValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMSchemaValidator.class);
    
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;
    
    public LLMSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }
    
    /**
     * Validation result.
     */
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private JsonNode data;
        
        public ValidationResult(boolean valid, List<String> errors, JsonNode data) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.data = data;
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public JsonNode getData() { return data; }
        
        public static ValidationResult success(JsonNode data) {
            return new ValidationResult(true, new ArrayList<>(), data);
        }
        
        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors, null);
        }
    }
    
    /**
     * Validate JSON response against a schema string.
     * 
     * @param jsonResponse The JSON response from LLM
     * @param schemaString The JSON schema as string
     * @return ValidationResult with success/failure and errors
     */
    public ValidationResult validate(String jsonResponse, String schemaString) {
        List<String> errors = new ArrayList<>();
        
        try {
            // Parse JSON response
            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(jsonResponse);
            } catch (Exception e) {
                errors.add("Invalid JSON: " + e.getMessage());
                return ValidationResult.failure(errors);
            }
            
            // Parse schema
            JsonSchema schema;
            try {
                JsonNode schemaNode = objectMapper.readTree(schemaString);
                schema = schemaFactory.getSchema(schemaNode);
            } catch (Exception e) {
                errors.add("Invalid schema: " + e.getMessage());
                logger.error("Failed to parse schema", e);
                return ValidationResult.failure(errors);
            }
            
            // Validate
            Set<ValidationMessage> validationMessages = schema.validate(jsonNode);
            
            if (validationMessages.isEmpty()) {
                logger.debug("Schema validation passed");
                return ValidationResult.success(jsonNode);
            } else {
                for (ValidationMessage msg : validationMessages) {
                    String error = String.format("%s: %s", msg.getPath(), msg.getMessage());
                    errors.add(error);
                    logger.warn("Schema validation error: {}", error);
                }
                return ValidationResult.failure(errors);
            }
            
        } catch (Exception e) {
            errors.add("Validation failed: " + e.getMessage());
            logger.error("Unexpected validation error", e);
            return ValidationResult.failure(errors);
        }
    }
    
    /**
     * Validate with retry recommendation.
     * Returns whether the error is retryable.
     */
    public boolean isRetryable(ValidationResult result) {
        if (result.isValid()) {
            return false; // No need to retry
        }
        
        // Check if errors are retryable
        for (String error : result.getErrors()) {
            String lowerError = error.toLowerCase();
            
            // Non-retryable errors (schema issues, not LLM issues)
            if (lowerError.contains("invalid schema") ||
                lowerError.contains("schema error")) {
                return false;
            }
            
            // Retryable errors (LLM formatting issues)
            if (lowerError.contains("required") ||
                lowerError.contains("type") ||
                lowerError.contains("format") ||
                lowerError.contains("enum") ||
                lowerError.contains("pattern")) {
                return true;
            }
        }
        
        // Default: retryable
        return true;
    }
    
    /**
     * Get user-friendly error message for validation failure.
     */
    public String getUserFriendlyError(ValidationResult result) {
        if (result.isValid()) {
            return null;
        }
        
        if (result.getErrors().isEmpty()) {
            return "Validation failed with unknown error";
        }
        
        // Categorize errors
        boolean hasMissingFields = false;
        boolean hasTypeErrors = false;
        boolean hasFormatErrors = false;
        
        for (String error : result.getErrors()) {
            String lowerError = error.toLowerCase();
            if (lowerError.contains("required")) hasMissingFields = true;
            if (lowerError.contains("type")) hasTypeErrors = true;
            if (lowerError.contains("format") || lowerError.contains("pattern")) hasFormatErrors = true;
        }
        
        StringBuilder message = new StringBuilder("The AI response was incomplete or malformed");
        
        if (hasMissingFields) {
            message.append(" (missing required fields)");
        }
        if (hasTypeErrors) {
            message.append(" (incorrect data types)");
        }
        if (hasFormatErrors) {
            message.append(" (invalid format)");
        }
        
        message.append(". Please try again.");
        
        return message.toString();
    }
    
    /**
     * Clean and validate JSON response.
     * Attempts to clean common LLM formatting issues before validation.
     */
    public ValidationResult cleanAndValidate(String jsonResponse, String schemaString) {
        // Try validation as-is first
        ValidationResult result = validate(jsonResponse, schemaString);
        if (result.isValid()) {
            return result;
        }
        
        logger.debug("Initial validation failed, attempting to clean response");
        
        // Try cleaning markdown
        String cleaned = cleanMarkdown(jsonResponse);
        result = validate(cleaned, schemaString);
        if (result.isValid()) {
            logger.debug("Validation passed after markdown cleaning");
            return result;
        }
        
        // Try extracting JSON object
        cleaned = extractJsonObject(jsonResponse);
        result = validate(cleaned, schemaString);
        if (result.isValid()) {
            logger.debug("Validation passed after JSON extraction");
            return result;
        }
        
        // All cleaning attempts failed
        logger.warn("Validation failed even after cleaning attempts");
        return result;
    }
    
    /**
     * Remove markdown code blocks from response.
     */
    private String cleanMarkdown(String response) {
        if (response == null) return "{}";
        
        String cleaned = response.trim();
        
        // Remove markdown code blocks
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        return cleaned.trim();
    }
    
    /**
     * Extract JSON object from response.
     */
    private String extractJsonObject(String response) {
        if (response == null) return "{}";
        
        String cleaned = response.trim();
        
        // Find JSON object boundaries
        int startIndex = cleaned.indexOf('{');
        int endIndex = cleaned.lastIndexOf('}');
        
        if (startIndex >= 0 && endIndex > startIndex) {
            return cleaned.substring(startIndex, endIndex + 1);
        }
        
        return cleaned;
    }
    
    /**
     * Validate and log detailed errors.
     */
    public ValidationResult validateWithLogging(String jsonResponse, String schemaString, 
                                                String context) {
        logger.debug("Validating LLM response for context: {}", context);
        
        ValidationResult result = cleanAndValidate(jsonResponse, schemaString);
        
        if (!result.isValid()) {
            logger.error("=== LLM SCHEMA VALIDATION FAILED ===");
            logger.error("Context: {}", context);
            logger.error("Errors:");
            for (String error : result.getErrors()) {
                logger.error("  - {}", error);
            }
            logger.error("Response preview: {}", 
                        jsonResponse.length() > 200 ? jsonResponse.substring(0, 200) + "..." : jsonResponse);
            logger.error("====================================");
        }
        
        return result;
    }
}
