package com.tripplanner.service.openrouter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.service.ai.CircuitBreaker;
import com.tripplanner.service.ai.exception.TransientAiException;
import com.tripplanner.service.ai.exception.PermanentAiException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
		value = "openrouter.api-key",
		matchIfMissing = false
)
public class OpenRouterClient implements AiClient {

	private static final Logger logger = LoggerFactory.getLogger(OpenRouterClient.class);

	@Value("${openrouter.base-url:https://openrouter.ai/api/v1}")
	private String baseUrl;

	@Value("${openrouter.api-key:}")
	private String apiKey;


	@Value("${ai.model:}")
	private String modelName;

	@Value("${ai.temperature:0.7}")
	private float temperature;

	@Value("${ai.max-tokens:8192}")
	private int maxTokens;

	@Value("${ai.mock-mode:false}")
	private boolean mockMode;

	@Value("${openrouter.timeout-seconds:60}")
	private int timeoutSeconds;

	private HttpClient httpClient;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private CircuitBreaker circuitBreaker;

	@PostConstruct
	public void initialize() {
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
		this.circuitBreaker = new CircuitBreaker("OpenRouterClient");
        String keyStatus = (apiKey == null || apiKey.isBlank()) ? "MISSING" : ("SET(len=" + apiKey.trim().length() + ")");
        logger.info("Initialized OpenRouter client with model: {} | apiKey={}",
                modelName, keyStatus);
	}

	@PreDestroy
	public void cleanup() {
		logger.info("OpenRouter client cleanup completed");
	}

	@Override
	public String generateContent(String userPrompt, String systemPrompt) {
		// Check circuit breaker before attempting request
		if (!circuitBreaker.allowRequest()) {
			logger.warn("Circuit breaker is OPEN for OpenRouterClient, throwing TransientAiException");
			throw new TransientAiException(
				"Circuit breaker is open for OpenRouter",
				"OpenRouterClient",
				503
			);
		}
		
		logger.info("🤖 OpenRouterClient: Starting content generation");
		logger.debug("Request details - Model: {}, Temperature: {}, Max tokens: {}", modelName, temperature, maxTokens);
		logger.debug("Circuit Breaker State: {}", circuitBreaker.getState());
		logger.debug("User prompt length: {} chars", userPrompt != null ? userPrompt.length() : 0);
		logger.debug("System prompt length: {} chars", systemPrompt != null ? systemPrompt.length() : 0);
		
		try {
			if (mockMode) {
				logger.info("OpenRouter mock mode enabled; returning empty response");
				circuitBreaker.recordSuccess();
				return "";
			}

			String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
			String body = buildChatCompletionsPayload(userPrompt, systemPrompt, false, null);
			
			logger.debug("API endpoint: {}", endpoint);
			logger.debug("Request payload size: {} chars", body.length());
			
			HttpRequest.Builder builder = HttpRequest.newBuilder()
					.uri(URI.create(endpoint))
					.timeout(Duration.ofSeconds(timeoutSeconds))
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + apiKey);

            // Attribution headers intentionally omitted

			HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(body)).build();

			logger.debug("Sending request to OpenRouter API...");
			long startTime = System.currentTimeMillis();
			
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			
			long responseTime = System.currentTimeMillis() - startTime;
			logger.info("OpenRouter API response: {} ({}ms)", response.statusCode(), responseTime);
			
			// Classify errors and throw typed exceptions
			if (isTransientError(response.statusCode())) {
				circuitBreaker.recordFailure();
				logger.error("❌ OpenRouter API transient error: {} - {}", response.statusCode(), response.body());
				throw new TransientAiException(
					"OpenRouter API returned transient error: " + response.statusCode(),
					"OpenRouterClient",
					response.statusCode()
				);
			}
			
			if (isPermanentError(response.statusCode())) {
				// Don't count permanent errors against circuit breaker
				logger.error("❌ OpenRouter API permanent error: {} - {}", response.statusCode(), response.body());
				throw new PermanentAiException(
					"OpenRouter API returned permanent error: " + response.statusCode(),
					"OpenRouterClient",
					response.statusCode()
				);
			}
			
			if (response.statusCode() != 200) {
				circuitBreaker.recordFailure();
				logger.error("❌ OpenRouter API unexpected error: {} - {}", response.statusCode(), response.body());
				throw new TransientAiException(
					"OpenRouter API returned unexpected status: " + response.statusCode(),
					"OpenRouterClient",
					response.statusCode()
				);
			}

			logger.debug("Response body size: {} chars", response.body().length());
			
			JsonNode root = objectMapper.readTree(response.body());
			JsonNode choices = root.get("choices");
			if (choices != null && choices.isArray() && choices.size() > 0) {
				JsonNode message = choices.get(0).get("message");
				if (message != null && message.get("content") != null) {
					String content = message.get("content").asText("");
					logger.info("✅ OpenRouter content generation successful - {} chars returned", content.length());
					circuitBreaker.recordSuccess();
					return content;
				}
			}
			
			logger.warn("❌ OpenRouter returned empty content - no valid choices found");
			logger.debug("Response structure: {}", root.toString());
			circuitBreaker.recordSuccess(); // Empty response is still a success
			return "";
		} catch (TransientAiException | PermanentAiException e) {
			// Re-throw typed exceptions as-is
			throw e;
		} catch (java.io.IOException | InterruptedException e) {
			// Network errors are transient
			circuitBreaker.recordFailure();
			logger.error("❌ OpenRouter network error: {}", e.getMessage(), e);
			throw new TransientAiException(
				"Network error calling OpenRouter: " + e.getMessage(),
				"OpenRouterClient",
				0,
				e
			);
		} catch (Exception e) {
			// Unknown errors are treated as transient
			circuitBreaker.recordFailure();
			logger.error("❌ OpenRouter content generation failed: {}", e.getMessage(), e);
			logger.error("Provider: OpenRouterClient, Model: {}, Available: {}", modelName, isAvailable());
			logger.error("Configuration - Base URL: {}, Timeout: {}s, Mock mode: {}", baseUrl, timeoutSeconds, mockMode);
			throw new TransientAiException(
				"Failed to generate content via OpenRouter: " + e.getMessage(),
				"OpenRouterClient",
				0,
				e
			);
		}
	}

	@Override
	public String generateStructuredContent(String userPrompt, String jsonSchema, String systemPrompt) {
		logger.info("🤖 OpenRouterClient: Starting structured content generation");
		logger.debug("Request details - Model: {}, Temperature: {}, Max tokens: {}", modelName, temperature, maxTokens);
		logger.debug("User prompt length: {} chars", userPrompt != null ? userPrompt.length() : 0);
		logger.debug("System prompt length: {} chars", systemPrompt != null ? systemPrompt.length() : 0);
		logger.debug("JSON schema length: {} chars", jsonSchema != null ? jsonSchema.length() : 0);
		
		try {
			if (mockMode) {
				logger.info("OpenRouter mock mode enabled; returning mock itinerary");
				return getMockItineraryResponse(userPrompt);
			}

			String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
			String body = buildChatCompletionsPayload(userPrompt, systemPrompt, true, jsonSchema);
			
			logger.debug("API endpoint: {}", endpoint);
			logger.debug("Request payload size: {} chars", body.length());
			
			HttpRequest.Builder builder = HttpRequest.newBuilder()
					.uri(URI.create(endpoint))
					.timeout(Duration.ofSeconds(timeoutSeconds))
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + apiKey);

            // Attribution headers intentionally omitted

			HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(body)).build();

			logger.debug("Sending structured request to OpenRouter API...");
			long startTime = System.currentTimeMillis();
			
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			
			long responseTime = System.currentTimeMillis() - startTime;
			logger.info("OpenRouter API structured response: {} ({}ms)", response.statusCode(), responseTime);
			
			if (response.statusCode() != 200) {
				logger.error("❌ OpenRouter API structured error: {} - {}", response.statusCode(), response.body());
				logger.error("Request that failed - endpoint: {}, payload size: {}", endpoint, body.length());
				throw new RuntimeException("OpenRouter API error: " + response.statusCode());
			}

			logger.debug("Response body size: {} chars", response.body().length());
			
			JsonNode root = objectMapper.readTree(response.body());
			JsonNode choices = root.get("choices");
			if (choices != null && choices.isArray() && choices.size() > 0) {
				JsonNode message = choices.get(0).get("message");
				if (message != null && message.get("content") != null) {
					String content = message.get("content").asText("");
					logger.info("✅ OpenRouter structured content generation successful - {} chars returned", content.length());
					return content;
				}
			}
			
			logger.warn("❌ OpenRouter returned empty structured content - no valid choices found");
			logger.debug("Response structure: {}", root.toString());
			return "";
		} catch (Exception e) {
			logger.error("❌ OpenRouter structured content generation failed: {}", e.getMessage(), e);
			logger.error("Provider: OpenRouterClient, Model: {}, Available: {}", modelName, isAvailable());
			logger.error("Configuration - Base URL: {}, Timeout: {}s, Mock mode: {}", baseUrl, timeoutSeconds, mockMode);
			throw new RuntimeException("Failed to generate structured content via OpenRouter: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean isAvailable() {
		return httpClient != null && apiKey != null && !apiKey.isBlank();
	}

	@Override
	public String getModelInfo() {
		return String.format("Provider: OpenRouter, Model: %s, Temperature: %.2f, Max Tokens: %d", modelName, temperature, maxTokens);
	}

	private String buildChatCompletionsPayload(String userPrompt, String systemPrompt, boolean jsonMode, String jsonSchema) {
		String system = systemPrompt == null ? "" : systemPrompt;
		String user = jsonMode
				? buildJsonModeUserPrompt(userPrompt, jsonSchema)
				: userPrompt;
		String responseFormat = "";
		if (jsonMode) {
			if (jsonSchema != null && !jsonSchema.isBlank()) {
				// Use json_schema to enforce schema strictly
				responseFormat = String.format(
						"\"response_format\": { \"type\": \"json_schema\", \"json_schema\": { \"name\": \"StructuredJson\", \"schema\": %s, \"strict\": true } },\n",
						jsonSchema.replace("\n", " "));
			} else {
				responseFormat = "\"response_format\": { \"type\": \"json_object\" },\n";
			}
		}

		return String.format(
				"""
				{
				  "model": "%s",
				  "temperature": %.2f,
				  "max_tokens": %d,
				  %s
				  "messages": [
				    %s
				  ]
				}
				""",
				modelName,
				temperature,
				maxTokens,
				responseFormat,
				buildMessages(system, user)
		);
	}

	private String buildMessages(String system, String user) {
		String sys = system == null || system.isBlank() ? null : system;
		StringBuilder sb = new StringBuilder();
		if (sys != null) {
			sb.append(String.format("{\"role\":\"system\",\"content\":%s},", jsonEscape(sys)));
		}
		sb.append(String.format("{\"role\":\"user\",\"content\":%s}", jsonEscape(user)));
		return sb.toString();
	}

	private String buildJsonModeUserPrompt(String userPrompt, String jsonSchema) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("You must respond with valid JSON that matches this exact schema:\n");
		if (jsonSchema != null && !jsonSchema.isBlank()) {
			prompt.append(jsonSchema).append("\n\n");
		}
		prompt.append("User request: ").append(userPrompt).append("\n\n");
		prompt.append("Respond with ONLY the JSON, no additional text or formatting.");
		return prompt.toString();
	}

	private String jsonEscape(String s) {
		return objectMapper.valueToTree(s).toString();
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
	 * Get mock itinerary response for testing/development.
	 */
	private String getMockItineraryResponse(String userPrompt) {
		// Extract destination from user prompt if possible
		String destination = "Barcelona"; // Default
		if (userPrompt.toLowerCase().contains("paris")) {
			destination = "Paris";
		} else if (userPrompt.toLowerCase().contains("london")) {
			destination = "London";
		} else if (userPrompt.toLowerCase().contains("rome")) {
			destination = "Rome";
		} else if (userPrompt.toLowerCase().contains("tokyo")) {
			destination = "Tokyo";
		}
		
		// Return a proper mock itinerary structure - simplified to avoid format issues
		long currentTimestamp = System.currentTimeMillis();
		return String.format("""
			{
			  "itineraryId": "it_mock_%s_%d",
			  "version": 1,
			  "summary": "3-day %s adventure with local attractions, cuisine, and cultural experiences",
			  "currency": "EUR",
			  "themes": ["culture", "food", "sightseeing"],
			  "days": [
			    {
			      "dayNumber": 1,
			      "date": "2025-06-01",
			      "location": "%s",
			      "nodes": [
			        {
			          "id": "n_arrival",
			          "type": "transport",
			          "title": "Arrival at %s",
			          "location": {
			            "name": "%s Airport",
			            "address": "%s City Center",
			            "coordinates": {
			              "lat": 41.3851,
			              "lng": 2.1734
			            }
			          },
			          "timing": {
			            "startTime": "2025-06-01T10:00:00Z",
			            "endTime": "2025-06-01T11:00:00Z",
			            "durationMin": 60
			          },
			          "cost": {
			            "amount": 0.0,
			            "currency": "EUR",
			            "per": "person"
			          },
			          "details": {
			            "rating": 4.0,
			            "category": "transport",
			            "tags": ["arrival", "airport"]
			          },
			          "labels": ["Arrival"],
			          "status": "planned",
			          "updatedBy": "system",
			          "updatedAt": %d
			        }
			      ]
			    }
			  ]
			}
			""", 
			destination.toLowerCase(), 
			currentTimestamp,
			destination,
			destination,
			destination,
			destination,
			destination,
			currentTimestamp
		);
	}
}


