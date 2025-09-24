package com.tripplanner.service.ai;

/**
 * Abstraction for Large Language Model providers.
 */
public interface AiClient {
	/**
	 * Generate content from a user prompt, with an optional system prompt.
	 */
	String generateContent(String userPrompt, String systemPrompt);

	/**
	 * Generate structured JSON content using a JSON schema hint and optional system prompt.
	 */
	String generateStructuredContent(String userPrompt, String jsonSchema, String systemPrompt);

	/**
	 * Whether the client is initialized and has necessary credentials.
	 */
	boolean isAvailable();

	/**
	 * Provider model information for diagnostics.
	 */
	String getModelInfo();
}


