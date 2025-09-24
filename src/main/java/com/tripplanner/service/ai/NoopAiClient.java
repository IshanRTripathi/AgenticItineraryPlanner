package com.tripplanner.service.ai;

/**
 * Safe no-op AI client used when no provider credentials are configured.
 */
public class NoopAiClient implements AiClient {
	@Override
	public String generateContent(String userPrompt, String systemPrompt) {
		return "";
	}

	@Override
	public String generateStructuredContent(String userPrompt, String jsonSchema, String systemPrompt) {
		return "{}";
	}

	@Override
	public boolean isAvailable() {
		return false;
	}

	@Override
	public String getModelInfo() {
		return "Provider: none, Model: none";
	}
}


