package com.tripplanner.config;

import com.tripplanner.filter.SessionIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

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

    @Bean
    public FilterRegistrationBean<SessionIdFilter> sessionIdFilterRegistration(SessionIdFilter filter) {
        FilterRegistrationBean<SessionIdFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1); // Run early, but after standard Spring filters
        return registration;
    }
}