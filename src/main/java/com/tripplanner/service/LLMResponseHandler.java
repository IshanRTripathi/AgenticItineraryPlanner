package com.tripplanner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Handles robust processing of LLM responses with JSON validation, repair, and continuation mechanisms.
 * Ensures that partial or malformed LLM outputs don't break the system.
 */
@Service
public class LLMResponseHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMResponseHandler.class);
    
    private final ObjectMapper objectMapper;
    
    // Finalization tokens that indicate complete responses
    private static final Set<String> FINALIZATION_TOKENS = Set.of(
        "RESPONSE_COMPLETE",
        "END_OF_RESPONSE", 
        "FINAL_ANSWER",
        "TASK_COMPLETE",
        "DONE"
    );
    
    // Common JSON repair patterns
    private static final Pattern INCOMPLETE_JSON_PATTERN = Pattern.compile(".*[{\\[]\\s*$", Pattern.DOTALL);
    private static final Pattern TRAILING_COMMA_PATTERN = Pattern.compile(",\\s*([}\\]])");
    private static final Pattern MISSING_QUOTES_PATTERN = Pattern.compile("([{,]\\s*)([a-zA-Z_][a-zA-Z0-9_]*)\\s*:");
    
    public LLMResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Process an LLM response with validation, repair, and continuation if needed.
     * 
     * @param response The raw LLM response
     * @param expectedSchema Optional JSON schema for validation
     * @param originalPrompt The original prompt (for continuation requests)
     * @return ProcessedResponse containing the validated and potentially repaired response
     */
    public ProcessedResponse processResponse(String response, JsonNode expectedSchema, String originalPrompt) {
        logger.debug("Processing LLM response of length: {}", response != null ? response.length() : 0);
        
        if (response == null || response.trim().isEmpty()) {
            return ProcessedResponse.failed("Empty or null response", ResponseFailureReason.EMPTY_RESPONSE);
        }
        
        // Check for finalization tokens
        boolean isComplete = hasFinalizationToken(response);
        
        // Extract JSON from response (handle cases where LLM adds explanatory text)
        String jsonContent = extractJsonContent(response);
        if (jsonContent == null) {
            return ProcessedResponse.failed("No JSON content found in response", ResponseFailureReason.NO_JSON_CONTENT);
        }
        
        // Attempt to parse JSON
        JsonNode parsedJson = parseJson(jsonContent);
        if (parsedJson != null) {
            // Validate against schema if provided
            if (expectedSchema != null) {
                ValidationResult validation = validateAgainstSchema(parsedJson, expectedSchema);
                if (!validation.isValid()) {
                    logger.warn("Schema validation failed: {}", validation.getErrors());
                    return ProcessedResponse.partialSuccess(parsedJson, validation.getErrors(), isComplete);
                }
            }
            
            return ProcessedResponse.success(parsedJson, isComplete);
        }
        
        // JSON parsing failed, attempt repair
        logger.info("JSON parsing failed, attempting repair");
        String repairedJson = repairJson(jsonContent);
        
        if (repairedJson != null) {
            JsonNode repairedParsed = parseJson(repairedJson);
            if (repairedParsed != null) {
                logger.info("JSON repair successful");
                
                // Validate repaired JSON
                if (expectedSchema != null) {
                    ValidationResult validation = validateAgainstSchema(repairedParsed, expectedSchema);
                    if (!validation.isValid()) {
                        return ProcessedResponse.partialSuccess(repairedParsed, validation.getErrors(), isComplete);
                    }
                }
                
                return ProcessedResponse.success(repairedParsed, isComplete);
            }
        }
        
        // If response appears truncated and we have original prompt, suggest continuation
        if (appearsTruncated(response) && originalPrompt != null) {
            String continuationPrompt = createContinuationPrompt(originalPrompt, response);
            return ProcessedResponse.needsContinuation(continuationPrompt, response);
        }
        
        return ProcessedResponse.failed("Unable to parse or repair JSON response", ResponseFailureReason.PARSE_FAILURE);
    }
    
    /**
     * Extract JSON content from LLM response that may contain additional text.
     */
    private String extractJsonContent(String response) {
        // Look for JSON objects or arrays
        int jsonStart = -1;
        int jsonEnd = -1;
        
        // Find the first { or [
        for (int i = 0; i < response.length(); i++) {
            char c = response.charAt(i);
            if (c == '{' || c == '[') {
                jsonStart = i;
                break;
            }
        }
        
        if (jsonStart == -1) {
            return null;
        }
        
        // Find the matching closing brace/bracket
        char openChar = response.charAt(jsonStart);
        char closeChar = openChar == '{' ? '}' : ']';
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = jsonStart; i < response.length(); i++) {
            char c = response.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == openChar) {
                    depth++;
                } else if (c == closeChar) {
                    depth--;
                    if (depth == 0) {
                        jsonEnd = i;
                        break;
                    }
                }
            }
        }
        
        if (jsonEnd == -1) {
            // JSON appears incomplete
            return response.substring(jsonStart);
        }
        
        return response.substring(jsonStart, jsonEnd + 1);
    }
    
    /**
     * Parse JSON string safely.
     */
    private JsonNode parseJson(String jsonString) {
        try {
            return objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            logger.debug("JSON parsing failed: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Attempt to repair malformed JSON.
     */
    private String repairJson(String jsonString) {
        String repaired = jsonString;
        
        // Remove trailing commas
        repaired = TRAILING_COMMA_PATTERN.matcher(repaired).replaceAll("$1");
        
        // Add missing quotes around property names
        repaired = MISSING_QUOTES_PATTERN.matcher(repaired).replaceAll("$1\"$2\":");
        
        // Balance braces and brackets
        repaired = balanceBraces(repaired);
        
        // Try parsing the repaired JSON
        if (parseJson(repaired) != null) {
            return repaired;
        }
        
        // If still failing, try more aggressive repairs
        return attemptAggressiveRepair(repaired);
    }
    
    /**
     * Balance braces and brackets in JSON string.
     */
    private String balanceBraces(String json) {
        int openBraces = 0;
        int openBrackets = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                switch (c) {
                    case '{':
                        openBraces++;
                        break;
                    case '}':
                        openBraces--;
                        break;
                    case '[':
                        openBrackets++;
                        break;
                    case ']':
                        openBrackets--;
                        break;
                }
            }
        }
        
        // Add missing closing braces/brackets
        StringBuilder result = new StringBuilder(json);
        for (int i = 0; i < openBrackets; i++) {
            result.append(']');
        }
        for (int i = 0; i < openBraces; i++) {
            result.append('}');
        }
        
        return result.toString();
    }
    
    /**
     * Attempt more aggressive JSON repair techniques.
     */
    private String attemptAggressiveRepair(String json) {
        // Try to complete incomplete strings
        if (json.endsWith("\"")) {
            // String might be incomplete, try adding a closing quote
            return json + "\"";
        }
        
        // Try to complete incomplete objects/arrays
        if (json.trim().endsWith(",")) {
            // Remove trailing comma and try to close
            String trimmed = json.trim();
            trimmed = trimmed.substring(0, trimmed.length() - 1);
            return balanceBraces(trimmed);
        }
        
        return json;
    }
    
    /**
     * Check if response has finalization tokens indicating completeness.
     */
    private boolean hasFinalizationToken(String response) {
        String upperResponse = response.toUpperCase();
        return FINALIZATION_TOKENS.stream().anyMatch(upperResponse::contains);
    }
    
    /**
     * Check if response appears to be truncated.
     */
    private boolean appearsTruncated(String response) {
        if (response == null || response.isEmpty()) {
            return false;
        }
        
        // Check for incomplete JSON patterns
        if (INCOMPLETE_JSON_PATTERN.matcher(response).matches()) {
            return true;
        }
        
        // Check if response ends abruptly without proper closure
        String trimmed = response.trim();
        return trimmed.endsWith(",") || 
               trimmed.endsWith(":") || 
               trimmed.endsWith("\"") && !trimmed.endsWith("\"\"");
    }
    
    /**
     * Create a continuation prompt for truncated responses.
     */
    private String createContinuationPrompt(String originalPrompt, String partialResponse) {
        return String.format(
            "The previous response was truncated. Please continue from where you left off.\n\n" +
            "Original prompt: %s\n\n" +
            "Partial response received: %s\n\n" +
            "Please provide the complete response, starting from where the previous response was cut off. " +
            "End your response with 'RESPONSE_COMPLETE' to indicate completion.",
            originalPrompt,
            partialResponse.length() > 500 ? partialResponse.substring(partialResponse.length() - 500) : partialResponse
        );
    }
    
    /**
     * Validate JSON against a schema.
     */
    private ValidationResult validateAgainstSchema(JsonNode json, JsonNode schema) {
        List<String> errors = new ArrayList<>();
        
        // Basic schema validation (simplified)
        if (schema.has("required")) {
            JsonNode required = schema.get("required");
            if (required.isArray()) {
                for (JsonNode requiredField : required) {
                    String fieldName = requiredField.asText();
                    if (!json.has(fieldName)) {
                        errors.add("Missing required field: " + fieldName);
                    }
                }
            }
        }
        
        if (schema.has("properties")) {
            JsonNode properties = schema.get("properties");
            properties.fieldNames().forEachRemaining(fieldName -> {
                if (json.has(fieldName)) {
                    JsonNode fieldSchema = properties.get(fieldName);
                    JsonNode fieldValue = json.get(fieldName);
                    
                    // Type validation
                    if (fieldSchema.has("type")) {
                        String expectedType = fieldSchema.get("type").asText();
                        if (!isCorrectType(fieldValue, expectedType)) {
                            errors.add("Field " + fieldName + " has incorrect type. Expected: " + expectedType);
                        }
                    }
                }
            });
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Check if a JSON node matches the expected type.
     */
    private boolean isCorrectType(JsonNode node, String expectedType) {
        switch (expectedType.toLowerCase()) {
            case "string":
                return node.isTextual();
            case "number":
            case "integer":
                return node.isNumber();
            case "boolean":
                return node.isBoolean();
            case "array":
                return node.isArray();
            case "object":
                return node.isObject();
            default:
                return true; // Unknown type, assume valid
        }
    }
    
    /**
     * Result of processing an LLM response.
     */
    public static class ProcessedResponse {
        private final boolean success;
        private final JsonNode data;
        private final List<String> errors;
        private final boolean isComplete;
        private final String continuationPrompt;
        private final String originalResponse;
        private final ResponseFailureReason failureReason;
        
        private ProcessedResponse(boolean success, JsonNode data, List<String> errors, 
                                boolean isComplete, String continuationPrompt, 
                                String originalResponse, ResponseFailureReason failureReason) {
            this.success = success;
            this.data = data;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
            this.isComplete = isComplete;
            this.continuationPrompt = continuationPrompt;
            this.originalResponse = originalResponse;
            this.failureReason = failureReason;
        }
        
        public static ProcessedResponse success(JsonNode data, boolean isComplete) {
            return new ProcessedResponse(true, data, null, isComplete, null, null, null);
        }
        
        public static ProcessedResponse partialSuccess(JsonNode data, List<String> errors, boolean isComplete) {
            return new ProcessedResponse(true, data, errors, isComplete, null, null, null);
        }
        
        public static ProcessedResponse failed(String error, ResponseFailureReason reason) {
            return new ProcessedResponse(false, null, List.of(error), false, null, null, reason);
        }
        
        public static ProcessedResponse needsContinuation(String continuationPrompt, String originalResponse) {
            return new ProcessedResponse(false, null, null, false, continuationPrompt, originalResponse, 
                                       ResponseFailureReason.NEEDS_CONTINUATION);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public JsonNode getData() { return data; }
        public List<String> getErrors() { return errors; }
        public boolean isComplete() { return isComplete; }
        public String getContinuationPrompt() { return continuationPrompt; }
        public String getOriginalResponse() { return originalResponse; }
        public ResponseFailureReason getFailureReason() { return failureReason; }
        public boolean needsContinuation() { return continuationPrompt != null; }
    }
    
    /**
     * Result of schema validation.
     */
    private static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
    }
    
    /**
     * Reasons why response processing might fail.
     */
    public enum ResponseFailureReason {
        EMPTY_RESPONSE,
        NO_JSON_CONTENT,
        PARSE_FAILURE,
        SCHEMA_VALIDATION_FAILURE,
        NEEDS_CONTINUATION
    }
}