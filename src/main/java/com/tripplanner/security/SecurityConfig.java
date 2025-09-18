package com.tripplanner.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for the application.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    // CORS configuration is now hardcoded for simplicity
    
    private final GoogleIdTokenAuthFilter googleIdTokenAuthFilter;
    private final IdempotencyFilter idempotencyFilter;
    
    public SecurityConfig(GoogleIdTokenAuthFilter googleIdTokenAuthFilter, 
                         @org.springframework.beans.factory.annotation.Autowired(required = false) IdempotencyFilter idempotencyFilter) {
        this.googleIdTokenAuthFilter = googleIdTokenAuthFilter;
        this.idempotencyFilter = idempotencyFilter;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Completely disable CSRF for testing
            .csrf(AbstractHttpConfigurer::disable)
            
            // Session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Allow all requests for testing
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            
            // Disable default login/logout
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow all origins for testing
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // Set allowed methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Set allowed headers
        configuration.setAllowedHeaders(List.of("*"));
        
        // Set credentials
        configuration.setAllowCredentials(true);
        
        // Expose headers that the client can access
        configuration.setExposedHeaders(List.of(
            "X-Total-Count",
            "X-Page-Count",
            "X-Current-Page",
            "X-Request-ID",
            "X-Rate-Limit-Remaining",
            "X-Rate-Limit-Reset"
        ));
        
        // Max age for preflight requests
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
