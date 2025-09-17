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
    
    @Value("#{'${security.cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;
    
    @Value("#{'${security.cors.allowed-methods}'.split(',')}")
    private List<String> allowedMethods;
    
    @Value("#{'${security.cors.allowed-headers}'.split(',')}")
    private List<String> allowedHeaders;
    
    @Value("${security.cors.allow-credentials:true}")
    private boolean allowCredentials;
    
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
            
            // CSRF configuration - enable for state-changing operations
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                    "/auth/google",  // Google auth endpoint
                    "/payments/razorpay/webhook",  // Webhook endpoints
                    "/agents/stream"  // SSE endpoint
                )
            )
            
            // Session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers(HttpMethod.GET, "/itineraries/*/public").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/google").permitAll()
                .requestMatchers(HttpMethod.POST, "/payments/razorpay/webhook").permitAll()
                
                // Health check endpoints
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/info").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Add custom filters
            .addFilterBefore(googleIdTokenAuthFilter, UsernamePasswordAuthenticationFilter.class);
            
        // Add idempotency filter only if available
        if (idempotencyFilter != null) {
            http.addFilterBefore(idempotencyFilter, GoogleIdTokenAuthFilter.class);
        }
        
        http
            
            // Disable default login/logout
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins
        configuration.setAllowedOrigins(allowedOrigins);
        
        // Set allowed methods
        configuration.setAllowedMethods(allowedMethods);
        
        // Set allowed headers
        if (allowedHeaders.contains("*")) {
            configuration.setAllowedHeaders(List.of("*"));
        } else {
            configuration.setAllowedHeaders(allowedHeaders);
        }
        
        // Set credentials
        configuration.setAllowCredentials(allowCredentials);
        
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
