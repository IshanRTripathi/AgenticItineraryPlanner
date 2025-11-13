package com.tripplanner.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.tripplanner.util.GuestUserUtil;
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

    private static final Logger logger = LoggerFactory.getLogger(FirebaseAuthConfig.class);

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
            
            // Check if this is a guest-accessible endpoint
            if (isGuestAccessibleEndpoint(path)) {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    // Authenticated user - validate token
                    String idToken = authHeader.substring(7);
                    try {
                        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
                        String userId = decodedToken.getUid();
                        request.setAttribute("userId", userId);
                        request.setAttribute("userEmail", decodedToken.getEmail());
                        request.setAttribute("userName", decodedToken.getName());
                        request.setAttribute("isGuest", false);
                        logger.debug("Authenticated user: {} for guest-accessible endpoint: {}", userId, path);
                    } catch (Exception e) {
                        logger.warn("Invalid token for guest-accessible endpoint: {}, treating as guest", path);
                        // Invalid token - treat as guest
                        String guestUserId = GuestUserUtil.generateGuestUserId(request);
                        request.setAttribute("userId", guestUserId);
                        request.setAttribute("isGuest", true);
                    }
                } else {
                    // No auth header - guest user
                    String guestUserId = GuestUserUtil.generateGuestUserId(request);
                    request.setAttribute("userId", guestUserId);
                    request.setAttribute("isGuest", true);
                    logger.debug("Guest user accessing endpoint: {} with ID: {}", path, guestUserId);
                }
                filterChain.doFilter(request, response);
                return;
            }
            
            // For all other endpoints, require authentication
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

        /**
         * Check if an endpoint is accessible to guest users.
         * These endpoints work for both authenticated and guest users.
         */
        private boolean isGuestAccessibleEndpoint(String path) {
            return path.startsWith("/api/v1/itineraries") ||
                   path.startsWith("/api/v1/chat") ||
                   path.startsWith("/api/v1/tools") ||
                   path.matches(".*/nodes/.*") ||
                   path.matches(".*/workflow") ||
                   path.matches(".*/lock-states");
        }

        private boolean isPublicEndpoint(String path) {
            // Define public endpoints that don't require authentication
            return path.startsWith("/api/v1/health") ||
                   path.startsWith("/api/v1/public") ||
                   path.startsWith("/swagger") ||
                   path.startsWith("/v3/api-docs") ||
                   path.startsWith("/actuator") ||
                   path.equals("/") ||
                   path.equals("/favicon.ico");
        }

        // SSE endpoints removed - WebSocket handles real-time communication

    }

    // FirebaseSseAuthFilter removed - SSE endpoints no longer exist, WebSocket handles real-time communication

    /**
     * Register Firebase Auth Filter in the filter chain
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
