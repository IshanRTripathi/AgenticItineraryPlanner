package com.tripplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.service.ai.CircuitBreaker;
import com.tripplanner.service.ai.exception.TransientAiException;
import com.tripplanner.service.ai.exception.PermanentAiException;

/**
 * Service for interacting with Google's Gemini AI model via REST API.
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    value = "google.ai.api-key",
    matchIfMissing = false
)
public class GeminiClient implements AiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    
    // Retry configuration for transient errors (503, 429, etc.)
    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_RETRY_DELAY_MS = 2000; // 2 seconds
    private static final int MAX_RETRY_DELAY_MS = 10000; // 10 seconds
    
    @Value("${google.ai.api-key}")
    private String apiKey;
    
    @Value("${google.ai.model:gemini-2.5-flash}")
    private String modelName;
    
    @Value("${google.ai.temperature:0.7}")
    private float temperature;
    
    @Value("${google.ai.max-tokens:65535}")
    private int maxTokens;
    
    @Value("${google.ai.mock-mode:false}")
    private boolean mockMode;
    
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private CircuitBreaker circuitBreaker;
    
    @PostConstruct
    public void initialize() {
        logger.info("Initializing Gemini client with model: {}", modelName);
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .version(HttpClient.Version.HTTP_2)
                .build();
        
        this.objectMapper = new ObjectMapper();
        this.circuitBreaker = new CircuitBreaker("GeminiClient");
        
        logger.info("Gemini client initialized successfully with 150s request timeout");
    }
    
    @PreDestroy
    public void cleanup() {
        logger.info("Gemini client cleanup completed");
    }
    
    /**
     * Generate content using Gemini with a simple text prompt.
     */
    public String generateContent(String prompt) {
        return generateContent(prompt, null);
    }
    
    /**
     * Generate content using Gemini with a system prompt and user prompt.
     */
    @Override
    public String generateContent(String userPrompt, String systemPrompt) {
        return generateContentWithRetry(userPrompt, systemPrompt, 0);
    }
    
    /**
     * Generate content with circuit breaker and typed exception handling.
     * Note: Retry logic is now handled by ResilientAiClient based on RetryStrategy.
     */
    private String generateContentWithRetry(String userPrompt, String systemPrompt, int attemptNumber) {
        // Check circuit breaker before attempting request
        if (!circuitBreaker.allowRequest()) {
            logger.warn("Circuit breaker is OPEN for GeminiClient, throwing TransientAiException");
            throw new TransientAiException(
                "Circuit breaker is open for Gemini",
                "GeminiClient",
                503
            );
        }
        
        try {
            if (attemptNumber == 0) {
                logger.info("=== GEMINI CONTENT GENERATION REQUEST ===");
                logger.info("Model: {}", modelName);
                logger.info("Temperature: {}", temperature);
                logger.info("Max Tokens: {}", maxTokens);
                logger.info("Mock Mode: {}", mockMode);
                logger.info("Circuit Breaker State: {}", circuitBreaker.getState());
                logger.info("User Prompt Length: {}", userPrompt.length());
                logger.info("System Prompt Length: {}", systemPrompt != null ? systemPrompt.length() : 0);
                logger.info("User Prompt Preview: {}", userPrompt.length() > 200 ? userPrompt.substring(0, 200) + "..." : userPrompt);
            }
            
            // Build request payload
            String requestBody = buildRequestPayload(userPrompt, systemPrompt);
            if (attemptNumber == 0) {
                logger.info("Request Body Length: {}", requestBody.length());
            }
            
            String generatedText;
            
            if (mockMode) {
                logger.info("=== MOCK MODE: Returning cached response ===");
                generatedText = getMockResponse(userPrompt, systemPrompt);
                logger.info("Mock response length: {}", generatedText.length());
                circuitBreaker.recordSuccess();
            } else {
                // Make HTTP request to Gemini API with 150 second timeout
                String apiUrl = GEMINI_API_BASE_URL + modelName + ":generateContent";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl + "?key=" + apiKey))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(150))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                
                logger.info("Sending request to Gemini API...");
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                logger.info("=== GEMINI API RESPONSE ===");
                logger.info("Status Code: {}", response.statusCode());
                logger.info("Response Length: {}", response.body().length());
                logger.info("Response Body: {}", response.body());
                
                // Classify errors and throw typed exceptions
                if (isTransientError(response.statusCode())) {
                    circuitBreaker.recordFailure();
                    logger.error("Gemini API transient error: {} - {}", response.statusCode(), response.body());
                    throw new TransientAiException(
                        "Gemini API returned transient error: " + response.statusCode(),
                        "GeminiClient",
                        response.statusCode()
                    );
                }
                
                if (isPermanentError(response.statusCode())) {
                    // Don't count permanent errors against circuit breaker
                    logger.error("Gemini API permanent error: {} - {}", response.statusCode(), response.body());
                    throw new PermanentAiException(
                        "Gemini API returned permanent error: " + response.statusCode(),
                        "GeminiClient",
                        response.statusCode()
                    );
                }
                
                if (response.statusCode() != 200) {
                    circuitBreaker.recordFailure();
                    logger.error("Gemini API unexpected error: {} - {}", response.statusCode(), response.body());
                    throw new TransientAiException(
                        "Gemini API returned unexpected status: " + response.statusCode(),
                        "GeminiClient",
                        response.statusCode()
                    );
                }
                
                // Save the full response to file for later use
                saveResponseToFile(userPrompt, systemPrompt, response.body());
                
                // Parse response
                JsonNode responseJson = objectMapper.readTree(response.body());
                JsonNode candidates = responseJson.get("candidates");
                
                if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                    JsonNode firstCandidate = candidates.get(0);
                    JsonNode content = firstCandidate.get("content");
                    JsonNode parts = content.get("parts");
                    
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        generatedText = parts.get(0).get("text").asText();
                        
                        logger.info("=== GEMINI CONTENT GENERATION RESPONSE ===");
                        logger.info("Generated content length: {}", generatedText.length());
                        logger.info("Generated content preview: {}", 
                                   generatedText.length() > 300 ? generatedText.substring(0, 300) + "..." : generatedText);
                        logger.info("=========================================");
                        
                        circuitBreaker.recordSuccess();
                    } else {
                        logger.warn("No parts found in response");
                        generatedText = "";
                        circuitBreaker.recordSuccess(); // Empty response is still a success
                    }
                } else {
                    logger.warn("No content found in response");
                    generatedText = "";
                    circuitBreaker.recordSuccess(); // Empty response is still a success
                }
            }
            
            return generatedText;
            
        } catch (TransientAiException | PermanentAiException e) {
            // Re-throw typed exceptions as-is
            throw e;
        } catch (IOException | InterruptedException e) {
            // Network errors are transient
            circuitBreaker.recordFailure();
            logger.error("=== GEMINI NETWORK ERROR ===");
            logger.error("Error: {}", e.getMessage(), e);
            logger.error("============================");
            throw new TransientAiException(
                "Network error calling Gemini: " + e.getMessage(),
                "GeminiClient",
                0,
                e
            );
        } catch (Exception e) {
            // Unknown errors are treated as transient
            circuitBreaker.recordFailure();
            logger.error("=== GEMINI CONTENT GENERATION FAILED ===");
            logger.error("User Prompt: {}", userPrompt.length() > 100 ? userPrompt.substring(0, 100) + "..." : userPrompt);
            logger.error("Error: {}", e.getMessage(), e);
            logger.error("=======================================");
            throw new TransientAiException(
                "Failed to generate content with Gemini: " + e.getMessage(),
                "GeminiClient",
                0,
                e
            );
        }
    }
    
    /**
     * Build request payload for Gemini API.
     */
    private String buildRequestPayload(String userPrompt, String systemPrompt) throws IOException {
        StringBuilder fullPrompt = new StringBuilder();
        
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            fullPrompt.append(systemPrompt).append("\n\n");
        }
        
        fullPrompt.append(userPrompt);
        
        String requestJson = String.format("""
            {
              "contents": [{
                "parts": [{
                  "text": "%s"
                }]
              }],
              "generationConfig": {
                "temperature": %.2f,
                "maxOutputTokens": %d
              }
            }
            """, 
            fullPrompt.toString().replace("\"", "\\\"").replace("\n", "\\n"),
            temperature,
            maxTokens
        );
        
        return requestJson;
    }
    
    /**
     * Generate structured JSON content using Gemini.
     */
    @Override
    public String generateStructuredContent(String prompt, String jsonSchema, String systemPrompt) {
        String fullPrompt = buildStructuredPrompt(prompt, jsonSchema, systemPrompt);
        return generateContent(fullPrompt, null);
    }
    
    /**
     * Build a prompt for structured JSON output.
     */
    private String buildStructuredPrompt(String userPrompt, String jsonSchema, String systemPrompt) {
        StringBuilder promptBuilder = new StringBuilder();
        
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            promptBuilder.append(systemPrompt).append("\n\n");
        }
        
        promptBuilder.append("You must respond with valid JSON that matches this exact schema:\n");
        promptBuilder.append(jsonSchema).append("\n\n");
        promptBuilder.append("User request: ").append(userPrompt).append("\n\n");
        promptBuilder.append("Respond with ONLY the JSON, no additional text or formatting:");
        
        return promptBuilder.toString();
    }
    
    /**
     * Check if the Gemini client is available and properly configured.
     */
    @Override
    public boolean isAvailable() {
        return httpClient != null && apiKey != null && !apiKey.trim().isEmpty();
    }
    
    /**
     * Get model information.
     */
    @Override
    public String getModelInfo() {
        return String.format("Model: %s, Temperature: %.2f, Max Tokens: %d", 
                            modelName, temperature, maxTokens);
    }
    
    /**
     * Save Gemini response to file for later use in mock mode.
     */
    private void saveResponseToFile(String userPrompt, String systemPrompt, String responseBody) {
        try {
            // Create responses directory if it doesn't exist
            Path responsesDir = Paths.get("responses");
            if (!Files.exists(responsesDir)) {
                Files.createDirectories(responsesDir);
            }
            
            // Generate filename based on prompt hash and timestamp
            String promptHash = String.valueOf(userPrompt.hashCode());
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("gemini_response_%s_%s.json", promptHash, timestamp);
            Path responseFile = responsesDir.resolve(filename);
            
            // Create response object with metadata
            JsonNode responseJson = objectMapper.readTree(responseBody);
            JsonNode candidates = responseJson.get("candidates");
            String generatedText = "";
            
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.get("content");
                JsonNode parts = content.get("parts");
                
                if (parts != null && parts.isArray() && parts.size() > 0) {
                    generatedText = parts.get(0).get("text").asText();
                }
            }
            
            // Create metadata object
            String metadata = objectMapper.writeValueAsString(new ResponseMetadata(
                userPrompt, systemPrompt, generatedText, timestamp
            ));
            
            // Save to file
            Files.write(responseFile, metadata.getBytes());
            logger.info("Saved Gemini response to: {}", responseFile);
            
        } catch (Exception e) {
            logger.error("Failed to save response to file", e);
        }
    }
    
    /**
     * Get mock response from saved files.
     */
    private String getMockResponse(String userPrompt, String systemPrompt) {
        try {
            // Check for specific mock response files first
            Path mockResponsesDir = Paths.get("logs/gemini-responses");
            if (Files.exists(mockResponsesDir)) {
                // Look for Barcelona-specific mock response
                if (userPrompt.toLowerCase().contains("barcelona")) {
                    Path barcelonaFile = mockResponsesDir.resolve("barcelona_3day_family.json");
                    if (Files.exists(barcelonaFile)) {
                        String content = Files.readString(barcelonaFile);
                        logger.info("Using Barcelona mock response from: {}", barcelonaFile.getFileName());
                        return content;
                    }
                }
            }
            
            // Fallback to cached responses
            Path responsesDir = Paths.get("responses");
            if (!Files.exists(responsesDir)) {
                logger.warn("No responses directory found for mock mode");
                return getDefaultMockResponse();
            }
            
            // Look for the most recent response file
            String promptHash = String.valueOf(userPrompt.hashCode());
            String filenamePattern = "gemini_response_" + promptHash + "_";
            
            return Files.list(responsesDir)
                .filter(path -> path.getFileName().toString().startsWith(filenamePattern))
                .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                .findFirst()
                .map(path -> {
                    try {
                        String content = Files.readString(path);
                        ResponseMetadata metadata = objectMapper.readValue(content, ResponseMetadata.class);
                        logger.info("Using cached response from: {}", path.getFileName());
                        return metadata.generatedText;
                    } catch (Exception e) {
                        logger.error("Failed to read cached response from: {}", path, e);
                        return getDefaultMockResponse();
                    }
                })
                .orElseGet(() -> {
                    logger.warn("No cached response found for prompt hash: {}", promptHash);
                    return getDefaultMockResponse();
                });
                
        } catch (Exception e) {
            logger.error("Failed to get mock response", e);
            return getDefaultMockResponse();
        }
    }
    
    /**
     * Check if an HTTP status code represents a transient error.
     * Transient errors may succeed on retry or with a different provider.
     */
    private boolean isTransientError(int statusCode) {
        return statusCode == 503 || // Service Unavailable (overloaded)
               statusCode == 429 || // Too Many Requests (rate limit)
               statusCode == 500;   // Internal Server Error (may be transient)
    }
    
    /**
     * Check if an HTTP status code represents a permanent error.
     * Permanent errors indicate configuration or request problems that won't be fixed by retrying.
     */
    private boolean isPermanentError(int statusCode) {
        return statusCode == 401 || // Unauthorized (invalid API key)
               statusCode == 403 || // Forbidden (insufficient permissions)
               (statusCode >= 400 && statusCode < 500 && statusCode != 429); // Other client errors except rate limit
    }
    
    /**
     * Calculate retry delay with exponential backoff.
     * Note: This is kept for backward compatibility but retry logic is now in ResilientAiClient.
     */
    private int calculateRetryDelay(int attemptNumber) {
        int delay = INITIAL_RETRY_DELAY_MS * (int) Math.pow(2, attemptNumber);
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }
    
    /**
     * Get default mock response when no cached response is available.
     * Uses the proper normalized JSON structure.
     */
    private String getDefaultMockResponse() {
        try {
            // Load the proper normalized JSON response
            java.nio.file.Path jsonPath = java.nio.file.Paths.get("logs/gemini-responses/barcelona_3day_family_normalized.json");
            return java.nio.file.Files.readString(jsonPath);
        } catch (Exception e) {
            logger.error("Failed to load normalized mock response, using fallback: {}", e.getMessage());
            // Fallback to a minimal normalized structure
            return """
            {
              "itineraryId": "it_mock_fallback",
              "version": 1,
              "summary": "Mock itinerary response - fallback data",
              "currency": "EUR",
              "themes": ["culture", "architecture"],
              "days": [
                {
                  "dayNumber": 1,
                  "date": "2025-06-01",
                  "location": "Barcelona",
                  "nodes": [
                    {
                      "id": "n_mock_activity",
                      "type": "attraction",
                      "title": "Mock Activity",
                      "location": {
                        "name": "Mock Location",
                        "address": "Mock Address",
                        "coordinates": {
                          "lat": 41.3851,
                          "lng": 2.1734
                        }
                      },
                      "timing": {
                        "startTime": "2025-06-01T09:00:00Z",
                        "endTime": "2025-06-01T17:00:00Z",
                        "durationMin": 480
                      },
                      "cost": {
                        "amount": 50.0,
                        "currency": "EUR",
                        "perUnit": "person"
                      },
                      "details": {
                        "rating": 4.0,
                        "category": "attraction",
                        "tags": ["sightseeing"]
                      },
                      "labels": ["Mock"],
                      "status": "planned",
                      "updatedBy": "system",
                      "updatedAt": "2025-01-21T19:00:00Z"
                    }
                  ]
                }
              ]
            }
            """;
        }
    }
    
    /**
     * Metadata class for storing response information.
     */
    private static class ResponseMetadata {
        public String userPrompt;
        public String systemPrompt;
        public String generatedText;
        public String timestamp;
        
        public ResponseMetadata(String userPrompt, String systemPrompt, String generatedText, String timestamp) {
            this.userPrompt = userPrompt;
            this.systemPrompt = systemPrompt;
            this.generatedText = generatedText;
            this.timestamp = timestamp;
        }
    }
}
