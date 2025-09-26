package com.tripplanner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${FRONTEND_URL:}")
    private String frontendUrl;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Environment-aware configuration
        if ("cloud".equals(activeProfile) || "production".equals(activeProfile)) {
            // Production: Allow specific frontend URL and Cloud Run patterns
            if (frontendUrl != null && !frontendUrl.isEmpty()) {
                configuration.addAllowedOrigin(frontendUrl);
            }
            configuration.addAllowedOriginPattern("https://*.run.app");
            configuration.addAllowedOriginPattern("https://*.a.run.app");
        } else {
            // Development: Allow localhost and configured frontend URL
            List<String> devOrigins = Arrays.asList(
                "http://localhost:3000",
                "http://localhost:3001", 
                "http://127.0.0.1:3000",
                "http://127.0.0.1:3001"
            );
            configuration.setAllowedOrigins(devOrigins);
            
            if (frontendUrl != null && !frontendUrl.isEmpty()) {
                configuration.addAllowedOrigin(frontendUrl);
            }
        }
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
