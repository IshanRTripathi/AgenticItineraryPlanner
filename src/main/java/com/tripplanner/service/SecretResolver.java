package com.tripplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecretResolver {

	private static final Logger logger = LoggerFactory.getLogger(SecretResolver.class);

	public String resolve(String envVarName) {
		try {
			String value = System.getenv(envVarName);
			if (value != null && !value.isBlank()) {
				return value;
			}
			return System.getProperty(envVarName, "");
		} catch (Exception e) {
			logger.warn("Failed to resolve secret {}: {}", envVarName, e.getMessage());
			return "";
		}
	}
}


