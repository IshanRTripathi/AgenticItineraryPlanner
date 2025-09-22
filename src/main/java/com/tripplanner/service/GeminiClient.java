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

/**
 * Service for interacting with Google's Gemini AI model via REST API.
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    value = "google.ai.api-key",
    matchIfMissing = false
)
public class GeminiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent";
    
    @Value("${google.ai.api-key}")
    private String apiKey;
    
    @Value("${google.ai.model:gemini-1.5-pro}")
    private String modelName;
    
    @Value("${google.ai.temperature:0.7}")
    private float temperature;
    
    @Value("${google.ai.max-tokens:8192}")
    private int maxTokens;
    
    @Value("${google.ai.mock-mode:false}")
    private boolean mockMode;
    
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    
    @PostConstruct
    public void initialize() {
        logger.info("Initializing Gemini client with model: {}", modelName);
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        
        this.objectMapper = new ObjectMapper();
        
        logger.info("Gemini client initialized successfully");
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
    public String generateContent(String userPrompt, String systemPrompt) {
        try {
            logger.info("=== GEMINI CONTENT GENERATION REQUEST ===");
            logger.info("Model: {}", modelName);
            logger.info("Temperature: {}", temperature);
            logger.info("Max Tokens: {}", maxTokens);
            logger.info("Mock Mode: {}", mockMode);
            logger.info("User Prompt Length: {}", userPrompt.length());
            logger.info("System Prompt Length: {}", systemPrompt != null ? systemPrompt.length() : 0);
            logger.info("User Prompt Preview: {}", userPrompt.length() > 200 ? userPrompt.substring(0, 200) + "..." : userPrompt);
            
            // Build request payload
            String requestBody = buildRequestPayload(userPrompt, systemPrompt);
            logger.info("Request Body Length: {}", requestBody.length());
            
            String generatedText;
            
            if (mockMode) {
                logger.info("=== MOCK MODE: Returning cached response ===");
                generatedText = getMockResponse(userPrompt, systemPrompt);
                logger.info("Mock response length: {}", generatedText.length());
            } else {
                // Make HTTP request to Gemini API
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GEMINI_API_URL + "?key=" + apiKey))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(60))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                
                logger.info("Sending request to Gemini API...");
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                logger.info("=== GEMINI API RESPONSE ===");
                logger.info("Status Code: {}", response.statusCode());
                logger.info("Response Length: {}", response.body().length());
                
                if (response.statusCode() != 200) {
                    logger.error("Gemini API error: {} - {}", response.statusCode(), response.body());
                    throw new RuntimeException("Gemini API error: " + response.statusCode());
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
                    } else {
                        logger.warn("No parts found in response");
                        generatedText = "";
                    }
                } else {
                    logger.warn("No content found in response");
                    generatedText = "";
                }
            }
            
            return generatedText;
            
        } catch (Exception e) {
            logger.error("=== GEMINI CONTENT GENERATION FAILED ===");
            logger.error("User Prompt: {}", userPrompt.length() > 100 ? userPrompt.substring(0, 100) + "..." : userPrompt);
            logger.error("Error: {}", e.getMessage(), e);
            logger.error("=======================================");
            throw new RuntimeException("Failed to generate content with Gemini: " + e.getMessage(), e);
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
    public boolean isAvailable() {
        return httpClient != null && apiKey != null && !apiKey.trim().isEmpty();
    }
    
    /**
     * Get model information.
     */
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
