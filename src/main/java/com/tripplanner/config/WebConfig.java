package com.tripplanner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Web configuration for HTTP clients and related beans.
 */
@Configuration
public class WebConfig {

    /**
     * RestTemplate bean for making HTTP requests.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}