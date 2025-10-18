package com.tripplanner.config;

import com.tripplanner.service.GeminiClient;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.service.ai.ResilientAiClient;
import com.tripplanner.service.openrouter.OpenRouterClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.tripplanner.service.ai.NoopAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class AiClientConfig {

	private static final Logger logger = LoggerFactory.getLogger(AiClientConfig.class);

	@Value("${ai.provider:openrouter}")
	private String provider;

	@Value("${ai.model:}")
	private String aiModel;

	@Bean
	public AiClient aiClient(ObjectProvider<GeminiClient> geminiClientProvider,
	                        ObjectProvider<OpenRouterClient> openRouterClientProvider) {
		
		logger.info("=== AI CLIENT CONFIGURATION ===");
		logger.info("AI provider: {}", provider);
		logger.info("OpenRouter API key configured: {}", 
			System.getenv("OPENROUTER_API_KEY") != null || !System.getProperty("openrouter.api-key", "").isEmpty());
		logger.info("Gemini API key configured: {}", 
			System.getenv("GOOGLE_AI_API_KEY") != null || !System.getProperty("google.ai.api-key", "").isEmpty());
		
		// Get available providers
		OpenRouterClient openRouterClient = openRouterClientProvider.getIfAvailable();
		GeminiClient geminiClient = geminiClientProvider.getIfAvailable();
		
		logger.info("OpenRouter client available: {}", openRouterClient != null && openRouterClient.isAvailable());
		logger.info("Gemini client available: {}", geminiClient != null && geminiClient.isAvailable());
		
		// Build provider chain based on configuration
		List<AiClient> providers = new ArrayList<>();
		
		String normalized = provider == null ? "" : provider.trim().toLowerCase();
		switch (normalized) {
			case "openrouter":
				// OpenRouter first, Gemini as fallback
				if (openRouterClient != null) providers.add(openRouterClient);
				if (geminiClient != null) providers.add(geminiClient);
				logger.info("Creating ResilientAiClient with OpenRouter as primary, Gemini as fallback");
				break;
			case "gemini":
				// Gemini first, OpenRouter as fallback
				if (geminiClient != null) providers.add(geminiClient);
				if (openRouterClient != null) providers.add(openRouterClient);
				logger.info("Creating ResilientAiClient with Gemini as primary, OpenRouter as fallback");
				break;
			default:
				// Default: OpenRouter first, then Gemini
				if (openRouterClient != null) providers.add(openRouterClient);
				if (geminiClient != null) providers.add(geminiClient);
				logger.info("Creating ResilientAiClient with default provider chain (OpenRouter → Gemini)");
				break;
		}
		
		// If we have providers, create ResilientAiClient
		if (!providers.isEmpty()) {
			logger.info("Creating ResilientAiClient with {} providers", providers.size());
			ResilientAiClient resilientClient = new ResilientAiClient(providers);
			logger.info("ResilientAiClient created successfully");
			return resilientClient;
		}
		
		// Fallback to NoopAiClient for tests or local dev without keys
		logger.warn("No AI providers available, using NoopAiClient");
		logger.info("AI client configuration completed successfully");
		return new NoopAiClient();
	}
}


