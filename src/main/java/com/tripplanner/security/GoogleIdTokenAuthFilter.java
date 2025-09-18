package com.tripplanner.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

/**
 * Filter to verify Google ID tokens and set authentication context.
 */
@Component
public class GoogleIdTokenAuthFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(GoogleIdTokenAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String GOOGLE_ID_TOKEN_HEADER = "X-Google-ID-Token";
    
    @Value("${google.oauth.client-id}")
    private String clientId;
    
    private final GoogleIdTokenVerifier verifier;
    
    public GoogleIdTokenAuthFilter(@Value("${google.oauth.client-id}") String clientId) {
        this.clientId = clientId;
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory()
        )
        .setAudience(Collections.singletonList(clientId))
        .build();
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        logger.info("=== GOOGLE ID TOKEN AUTH FILTER ===");
        logger.info("Request URI: {}", request.getRequestURI());
        logger.info("Request Method: {}", request.getMethod());
        logger.info("Content Type: {}", request.getContentType());
        
        try {
            String token = extractToken(request);
            logger.info("Token extracted: {}", token != null ? "YES (length: " + token.length() + ")" : "NO");
            
            if (token != null) {
                GoogleIdToken idToken = verifyToken(token);
                
                if (idToken != null) {
                    setAuthentication(idToken);
                    logger.info("Successfully authenticated user: {}", idToken.getPayload().getEmail());
                } else {
                    logger.warn("Invalid Google ID token");
                }
            } else {
                logger.info("No authentication token found, proceeding without authentication");
            }
            
        } catch (Exception e) {
            logger.error("Error processing Google ID token", e);
            // Continue without authentication - let Spring Security handle unauthorized requests
        }
        
        logger.info("=== AUTH FILTER COMPLETED ===");
        logger.info("Proceeding to next filter");
        logger.info("============================");
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract token from request headers.
     */
    private String extractToken(HttpServletRequest request) {
        // Try Authorization header first
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        // Try custom Google ID token header
        String googleTokenHeader = request.getHeader(GOOGLE_ID_TOKEN_HEADER);
        if (googleTokenHeader != null) {
            return googleTokenHeader;
        }
        
        return null;
    }
    
    /**
     * Verify Google ID token.
     */
    private GoogleIdToken verifyToken(String tokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(tokenString);
            
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                
                // Verify token is not expired
                if (payload.getExpirationTimeSeconds() * 1000 < System.currentTimeMillis()) {
                    logger.warn("Google ID token has expired");
                    return null;
                }
                
                // Verify email is verified
                Boolean emailVerified = payload.getEmailVerified();
                if (emailVerified == null || !emailVerified) {
                    logger.warn("Google account email is not verified");
                    return null;
                }
                
                // Additional validation can be added here
                
                return idToken;
            }
            
        } catch (GeneralSecurityException | IOException e) {
            logger.error("Failed to verify Google ID token", e);
        }
        
        return null;
    }
    
    /**
     * Set authentication context.
     */
    private void setAuthentication(GoogleIdToken idToken) {
        GoogleIdToken.Payload payload = idToken.getPayload();
        
        String userId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");
        
        // Create user principal
        GoogleUserPrincipal principal = new GoogleUserPrincipal(
                userId, email, name, pictureUrl
        );
        
        // Create authentication token
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
        
        // Set authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Skip filter for public endpoints
        return path.equals("/api/v1/auth/google") ||
               path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/info") ||
               path.endsWith("/public") ||
               path.contains("/payments/razorpay/webhook");
    }
}

