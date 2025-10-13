package com.tripplanner.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Firebase Authentication Configuration
 * Validates Firebase ID tokens for authenticated requests
 */
@Configuration
@ConditionalOnProperty(name = "firebase.auth.enabled", havingValue = "true", matchIfMissing = false)
public class FirebaseAuthConfig {

    @Autowired
    private FirebaseAuth firebaseAuth;

    /**
     * Filter to validate Firebase ID tokens
     */
    public static class FirebaseAuthFilter extends OncePerRequestFilter {

        private static final Logger logger = LoggerFactory.getLogger(FirebaseAuthFilter.class);
        private final FirebaseAuth firebaseAuth;

        public FirebaseAuthFilter(FirebaseAuth firebaseAuth) {
            this.firebaseAuth = firebaseAuth;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            
            String path = request.getRequestURI();
            String method = request.getMethod();
            
            // Allow OPTIONS requests (CORS preflight) to bypass authentication
            if ("OPTIONS".equals(method)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // Skip authentication for public endpoints
            if (isPublicEndpoint(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            String authHeader = request.getHeader("Authorization");
            
            // Handle optional authentication for /json endpoints
            if (path.endsWith("/json")) {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String idToken = authHeader.substring(7);
                    try {
                        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
                        String userId = decodedToken.getUid();
                        request.setAttribute("userId", userId);
                        request.setAttribute("userEmail", decodedToken.getEmail());
                        request.setAttribute("userName", decodedToken.getName());
                        logger.debug("Authenticated user: {} for /json endpoint: {}", userId, path);
                    } catch (Exception e) {
                        logger.warn("Invalid token for /json endpoint: {}", path, e);
                        // Continue without authentication for /json endpoints
                    }
                }
                filterChain.doFilter(request, response);
                return;
            }
            
            // Handle optional authentication for lock endpoints (for testing)
            if (path.matches(".*/nodes/.*/lock") || path.matches(".*/lock-states")) {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String idToken = authHeader.substring(7);
                    try {
                        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
                        String userId = decodedToken.getUid();
                        request.setAttribute("userId", userId);
                        request.setAttribute("userEmail", decodedToken.getEmail());
                        request.setAttribute("userName", decodedToken.getName());
                        logger.debug("Authenticated user: {} for lock endpoint: {}", userId, path);
                    } catch (Exception e) {
                        logger.warn("Invalid token for lock endpoint: {}, continuing without auth", path);
                        // Continue without authentication for lock endpoints during testing
                    }
                } else {
                    logger.debug("No auth header for lock endpoint: {}, allowing anonymous access for testing", path);
                }
                filterChain.doFilter(request, response);
                return;
            }
            
            // Handle optional authentication for workflow sync endpoints (for development/testing)
            if (path.matches(".*/workflow") || path.matches(".*/nodes/.*")) {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String idToken = authHeader.substring(7);
                    try {
                        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
                        String userId = decodedToken.getUid();
                        request.setAttribute("userId", userId);
                        request.setAttribute("userEmail", decodedToken.getEmail());
                        request.setAttribute("userName", decodedToken.getName());
                        logger.debug("Authenticated user: {} for workflow sync endpoint: {}", userId, path);
                    } catch (Exception e) {
                        logger.warn("Invalid token for workflow sync endpoint: {}, continuing without auth", path);
                        // Continue without authentication for workflow sync during development
                    }
                } else {
                    logger.debug("No auth header for workflow sync endpoint: {}, allowing anonymous access for development", path);
                }
                filterChain.doFilter(request, response);
                return;
            }
            
            // Handle optional authentication for chat endpoints (for development/testing)
            if (path.matches(".*/chat.*")) {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String idToken = authHeader.substring(7);
                    try {
                        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
                        String userId = decodedToken.getUid();
                        request.setAttribute("userId", userId);
                        request.setAttribute("userEmail", decodedToken.getEmail());
                        request.setAttribute("userName", decodedToken.getName());
                        logger.debug("Authenticated user: {} for chat endpoint: {}", userId, path);
                    } catch (Exception e) {
                        logger.warn("Invalid token for chat endpoint: {}, continuing without auth", path);
                        // Set a test user ID for development when token is invalid
                        request.setAttribute("userId", "test-user");
                    }
                } else {
                    logger.debug("No auth header for chat endpoint: {}, allowing anonymous access for development", path);
                    // Set a test user ID for development
                    request.setAttribute("userId", "test-user");
                }
                filterChain.doFilter(request, response);
                return;
            }
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Missing or invalid Authorization header for path: {}", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
                return;
            }

            String idToken = authHeader.substring(7); // Remove "Bearer " prefix

            try {
                FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
                String userId = decodedToken.getUid();
                
                // Add user ID to request attributes for use in controllers
                request.setAttribute("userId", userId);
                request.setAttribute("userEmail", decodedToken.getEmail());
                request.setAttribute("userName", decodedToken.getName());
                
                logger.debug("Authenticated user: {} for path: {}", userId, path);
                filterChain.doFilter(request, response);
                
            } catch (Exception e) {
                logger.error("Firebase token verification failed for path: {}", path, e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
            }
        }

        private boolean isPublicEndpoint(String path) {
            // Define public endpoints that don't require authentication
            return path.startsWith("/api/v1/health") ||
                   path.startsWith("/api/v1/public") ||
                   path.startsWith("/api/v1/agents/stream") ||  // Allow SSE stream endpoint
                   path.startsWith("/api/v1/agents/events/") ||  // Allow SSE events endpoint
                   path.startsWith("/api/v1/chat/route") ||  // Allow chat route endpoint
                   path.matches(".*/nodes/.*/lock") ||  // Allow node lock endpoint for testing
                   path.matches(".*/lock-states") ||  // Allow lock states endpoint for debugging
                   path.startsWith("/swagger") ||
                   path.startsWith("/v3/api-docs") ||
                   path.startsWith("/actuator") ||
                   path.equals("/") ||
                   path.equals("/favicon.ico");
        }

    }

    /**
     * Register Firebase Auth Filter in the filter chain
     * Temporarily disabled for development
     */
    @Bean
    public FilterRegistrationBean<FirebaseAuthFilter> firebaseAuthFilterRegistration() {
        FilterRegistrationBean<FirebaseAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new FirebaseAuthFilter(firebaseAuth));
        registration.addUrlPatterns("/api/v1/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("firebaseAuthFilter");
        return registration;
    }

    /**
     * Get Firebase Auth instance
     */
    public FirebaseAuth getFirebaseAuth() {
        return firebaseAuth;
    }
}
