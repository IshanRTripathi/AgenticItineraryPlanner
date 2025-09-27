package com.tripplanner.service.openrouter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.service.ai.AiClient;
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

	@PostConstruct
	public void initialize() {
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
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
		try {
			if (mockMode) {
				logger.info("OpenRouter mock mode enabled; returning empty response");
				return "";
			}

			String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
			String body = buildChatCompletionsPayload(userPrompt, systemPrompt, false, null);
			HttpRequest.Builder builder = HttpRequest.newBuilder()
					.uri(URI.create(endpoint))
					.timeout(Duration.ofSeconds(timeoutSeconds))
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + apiKey);

            // Attribution headers intentionally omitted

			HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(body)).build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				logger.error("OpenRouter API error: {} - {}", response.statusCode(), response.body());
				throw new RuntimeException("OpenRouter API error: " + response.statusCode());
			}

			JsonNode root = objectMapper.readTree(response.body());
			JsonNode choices = root.get("choices");
			if (choices != null && choices.isArray() && choices.size() > 0) {
				JsonNode message = choices.get(0).get("message");
				if (message != null && message.get("content") != null) {
					return message.get("content").asText("");
				}
			}
			return "";
		} catch (Exception e) {
			logger.error("Failed to generate content via OpenRouter", e);
			throw new RuntimeException("Failed to generate content via OpenRouter: " + e.getMessage(), e);
		}
	}

	@Override
	public String generateStructuredContent(String userPrompt, String jsonSchema, String systemPrompt) {
		try {
		if (mockMode) {
			logger.info("OpenRouter mock mode enabled; returning mock itinerary");
			return getMockItineraryResponse(userPrompt);
		}

			String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
			String body = buildChatCompletionsPayload(userPrompt, systemPrompt, true, jsonSchema);
			HttpRequest.Builder builder = HttpRequest.newBuilder()
					.uri(URI.create(endpoint))
					.timeout(Duration.ofSeconds(timeoutSeconds))
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + apiKey);

            // Attribution headers intentionally omitted

			HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(body)).build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				logger.error("OpenRouter API error: {} - {}", response.statusCode(), response.body());
				throw new RuntimeException("OpenRouter API error: " + response.statusCode());
			}

			JsonNode root = objectMapper.readTree(response.body());
			JsonNode choices = root.get("choices");
			if (choices != null && choices.isArray() && choices.size() > 0) {
				JsonNode message = choices.get(0).get("message");
				if (message != null && message.get("content") != null) {
					return message.get("content").asText("");
				}
			}
			return "";
		} catch (Exception e) {
			logger.error("Failed to generate structured content via OpenRouter", e);
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
		
		// Return a proper mock itinerary structure
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
			            "address": "%s",
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
			          "updatedAt": "2025-01-21T19:00:00Z"
			        },
			        {
			          "id": "n_attraction_1",
			          "type": "attraction",
			          "title": "Main Attraction in %s",
			          "location": {
			            "name": "Main Attraction",
			            "address": "%s",
			            "coordinates": {
			              "lat": 41.3851,
			              "lng": 2.1734
			            }
			          },
			          "timing": {
			            "startTime": "2025-06-01T14:00:00Z",
			            "endTime": "2025-06-01T17:00:00Z",
			            "durationMin": 180
			          },
			          "cost": {
			            "amount": 25.0,
			            "currency": "EUR",
			            "per": "person"
			          },
			          "details": {
			            "rating": 4.5,
			            "category": "attraction",
			            "tags": ["sightseeing", "culture"]
			          },
			          "labels": ["Must-Visit"],
			          "status": "planned",
			          "updatedBy": "system",
			          "updatedAt": "2025-01-21T19:00:00Z"
			        }
			      ]
			    },
			    {
			      "dayNumber": 2,
			      "date": "2025-06-02",
			      "location": "%s",
			      "nodes": [
			        {
			          "id": "n_meal_1",
			          "type": "meal",
			          "title": "Local Restaurant",
			          "location": {
			            "name": "Local Restaurant",
			            "address": "%s",
			            "coordinates": {
			              "lat": 41.3851,
			              "lng": 2.1734
			            }
			          },
			          "timing": {
			            "startTime": "2025-06-02T12:00:00Z",
			            "endTime": "2025-06-02T14:00:00Z",
			            "durationMin": 120
			          },
			          "cost": {
			            "amount": 30.0,
			            "currency": "EUR",
			            "per": "person"
			          },
			          "details": {
			            "rating": 4.2,
			            "category": "meal",
			            "tags": ["local", "cuisine"]
			          },
			          "labels": ["Local Experience"],
			          "status": "planned",
			          "updatedBy": "system",
			          "updatedAt": "2025-01-21T19:00:00Z"
			        }
			      ]
			    },
			    {
			      "dayNumber": 3,
			      "date": "2025-06-03",
			      "location": "%s",
			      "nodes": [
			        {
			          "id": "n_departure",
			          "type": "transport",
			          "title": "Departure from %s",
			          "location": {
			            "name": "%s Airport",
			            "address": "%s",
			            "coordinates": {
			              "lat": 41.3851,
			              "lng": 2.1734
			            }
			          },
			          "timing": {
			            "startTime": "2025-06-03T17:00:00Z",
			            "endTime": "2025-06-03T18:00:00Z",
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
			            "tags": ["departure", "airport"]
			          },
			          "labels": ["Departure"],
			          "status": "planned",
			          "updatedBy": "system",
			          "updatedAt": "2025-01-21T19:00:00Z"
			        }
			      ]
			    }
			  ]
			}
			""", 
			destination.toLowerCase(), 
			System.currentTimeMillis(),
			destination,
			destination,
			destination,
			destination,
			destination,
			destination,
			destination,
			destination,
			destination,
			destination,
			destination,
			destination
		);
	}
}


