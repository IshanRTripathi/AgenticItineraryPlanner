package com.tripplanner.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Google Cloud Pub/Sub integration.
 * Used for async analytics event ingestion.
 * 
 * PubSubTemplate is auto-configured by spring-cloud-gcp-starter-pubsub.
 * No additional beans needed - we use PubSubTemplate directly in controllers.
 */
@Configuration
public class PubSubConfig {
    // PubSubTemplate is auto-configured by Spring Cloud GCP
    // No additional configuration needed
}
