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

	@Value("${openrouter.app-url:}")
	private String appUrl;

	@Value("${openrouter.app-title:}")
	private String appTitle;

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
		logger.info("Initialized OpenRouter client with model: {}", modelName);
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

			// Optional attribution headers
			if (appUrl != null && !appUrl.isBlank()) {
				builder.header("HTTP-Referer", appUrl);
			}
			if (appTitle != null && !appTitle.isBlank()) {
				builder.header("X-Title", appTitle);
			}

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
				logger.info("OpenRouter mock mode enabled; returning empty JSON");
				return "{}";
			}

			String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
			String body = buildChatCompletionsPayload(userPrompt, systemPrompt, true, jsonSchema);
			HttpRequest.Builder builder = HttpRequest.newBuilder()
					.uri(URI.create(endpoint))
					.timeout(Duration.ofSeconds(timeoutSeconds))
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + apiKey);

			// Optional attribution headers
			if (appUrl != null && !appUrl.isBlank()) {
				builder.header("HTTP-Referer", appUrl);
			}
			if (appTitle != null && !appTitle.isBlank()) {
				builder.header("X-Title", appTitle);
			}

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
}


