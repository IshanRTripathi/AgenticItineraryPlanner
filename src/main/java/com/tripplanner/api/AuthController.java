package com.tripplanner.api;

import com.tripplanner.security.GoogleUserPrincipal;
import com.tripplanner.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    /**
     * Exchange Google ID token for application session.
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        logger.info("Processing Google authentication request");
        
        AuthResponse response = authService.authenticateWithGoogle(request.idToken());
        
        logger.info("Google authentication successful for user: {}", response.user().email());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get current user information.
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfo> getCurrentUser(@AuthenticationPrincipal GoogleUserPrincipal user) {
        logger.debug("Getting current user info for: {}", user.getUserId());
        
        UserInfo userInfo = new UserInfo(
                user.getUserId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPictureUrl()
        );
        
        return ResponseEntity.ok(userInfo);
    }
    
    /**
     * Refresh user session/token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@AuthenticationPrincipal GoogleUserPrincipal user) {
        logger.debug("Refreshing session for user: {}", user.getUserId());
        
        AuthResponse response = authService.refreshSession(user);
        
        logger.debug("Session refreshed for user: {}", user.getUserId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Logout user (invalidate session).
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal GoogleUserPrincipal user) {
        logger.info("Logging out user: {}", user.getUserId());
        
        authService.logout(user);
        
        logger.info("User logged out: {}", user.getUserId());
        return ResponseEntity.ok().build();
    }
    
    /**
     * Request DTO for Google authentication.
     */
    public record GoogleAuthRequest(
            @NotBlank(message = "ID token is required")
            String idToken
    ) {}
    
    /**
     * Response DTO for authentication.
     */
    public record AuthResponse(
            String session,
            UserInfo user,
            java.time.Instant expiresAt
    ) {}
    
    /**
     * DTO for user information.
     */
    public record UserInfo(
            String id,
            String email,
            String name,
            String pictureUrl
    ) {}
}

