package com.tripplanner.config;

import com.tripplanner.service.GeminiClient;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.service.openrouter.OpenRouterClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.tripplanner.service.ai.NoopAiClient;

@Configuration
public class AiClientConfig {

	@Value("${ai.provider:gemini}")
	private String provider;

	@Value("${ai.model:}")
	private String aiModel;

	@Bean
	public AiClient aiClient(ObjectProvider<GeminiClient> geminiClientProvider,
	                        ObjectProvider<OpenRouterClient> openRouterClientProvider) {
		String normalized = provider == null ? "" : provider.trim().toLowerCase();
		switch (normalized) {
			case "openrouter":
				OpenRouterClient orc = openRouterClientProvider.getIfAvailable();
				if (orc != null && orc.isAvailable()) {
					return orc;
				}
				break;
			case "gemini":
			case "":
				GeminiClient gc = geminiClientProvider.getIfAvailable();
				if (gc != null && gc.isAvailable()) {
					return gc;
				}
				break;
			default:
				break;
		}
		// Fallbacks: prefer any available client
		OpenRouterClient orFallback = openRouterClientProvider.getIfAvailable();
		if (orFallback != null && orFallback.isAvailable()) {
			return orFallback;
		}
		GeminiClient gFallback = geminiClientProvider.getIfAvailable();
		if (gFallback != null && gFallback.isAvailable()) {
			return gFallback;
		}
		// During tests or local dev without keys, provide a safe no-op client
		return new NoopAiClient();
	}
}


