package com.tripplanner.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Utility class for guest user management.
 * Provides methods for generating and validating guest user IDs.
 */
public class GuestUserUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(GuestUserUtil.class);
    
    private static final String GUEST_PREFIX = "guest_";
    private static final String GUEST_SESSION_HEADER = "X-Guest-Session-Id";
    
    /**
     * Check if a user ID represents a guest user.
     * 
     * @param userId The user ID to check
     * @return true if the user ID is "anonymous"
     */
    public static boolean isGuestUser(String userId) {
        return "anonymous".equals(userId);
    }
    
    /**
     * Generate a guest user ID from an HTTP request.
     * Always returns "anonymous" for guest users.
     * 
     * @param request The HTTP request
     * @return "anonymous" for guest users
     */
    public static String generateGuestUserId(HttpServletRequest request) {
        // Check for X-Guest-Session-Id header from frontend
        String guestSessionId = request.getHeader(GUEST_SESSION_HEADER);
        
        if (guestSessionId != null && !guestSessionId.isBlank()) {
            // Frontend sent guest session ID (should be "anonymous")
            logger.debug("Guest user accessing with session ID: {}", guestSessionId);
            return "anonymous";
        }
        
        // Default to anonymous
        logger.debug("Guest user accessing without session header, using anonymous");
        return "anonymous";
    }
    
    /**
     * Validate that a guest user ID is properly formatted.
     * 
     * @param guestUserId The guest user ID to validate
     * @return true if the guest user ID is "anonymous"
     */
    public static boolean isValidGuestUserId(String guestUserId) {
        return "anonymous".equals(guestUserId);
    }
}
