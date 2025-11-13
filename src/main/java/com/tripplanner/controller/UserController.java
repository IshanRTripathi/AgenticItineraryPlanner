package com.tripplanner.controller;

import com.tripplanner.service.UserDataService;
import com.tripplanner.util.GuestUserUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for user-related operations.
 * Handles guest user migration and user data management.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private final UserDataService userDataService;
    
    public UserController(UserDataService userDataService) {
        this.userDataService = userDataService;
    }
    
    /**
     * Migrate guest user data to authenticated user.
     * POST /api/v1/users/migrate-guest
     * 
     * This endpoint is called when a guest user signs in.
     * It transfers all itineraries from the guest session to the authenticated user.
     */
    @PostMapping("/migrate-guest")
    public ResponseEntity<MigrationResponse> migrateGuestData(
            @Valid @RequestBody MigrationRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("=== GUEST DATA MIGRATION REQUEST ===");
        logger.info("Guest User ID: {}", request.getGuestUserId());
        
        try {
            // Extract authenticated user ID from request attributes
            String authenticatedUserId = (String) httpRequest.getAttribute("userId");
            
            if (authenticatedUserId == null) {
                logger.error("No authenticated user ID found in request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(MigrationResponse.error("Authentication required"));
            }
            
            // Validate that the authenticated user is not a guest
            if (GuestUserUtil.isGuestUser(authenticatedUserId)) {
                logger.error("Authenticated user ID is a guest user: {}", authenticatedUserId);
                return ResponseEntity.badRequest()
                        .body(MigrationResponse.error("Cannot migrate to a guest user"));
            }
            
            // Validate guest user ID
            String guestUserId = request.getGuestUserId();
            if (guestUserId == null || guestUserId.isBlank()) {
                logger.error("Guest user ID is missing");
                return ResponseEntity.badRequest()
                        .body(MigrationResponse.error("Guest user ID is required"));
            }
            
            if (!GuestUserUtil.isGuestUser(guestUserId)) {
                logger.error("Provided user ID is not a guest user: {}", guestUserId);
                return ResponseEntity.badRequest()
                        .body(MigrationResponse.error("Invalid guest user ID"));
            }
            
            // Perform migration
            logger.info("Migrating data from guest {} to authenticated user {}", 
                       guestUserId, authenticatedUserId);
            
            int migratedCount = userDataService.migrateGuestDataToUser(guestUserId, authenticatedUserId);
            
            logger.info("=== MIGRATION COMPLETE ===");
            logger.info("Migrated {} itineraries", migratedCount);
            logger.info("From: {}", guestUserId);
            logger.info("To: {}", authenticatedUserId);
            logger.info("==========================");
            
            return ResponseEntity.ok(MigrationResponse.success(migratedCount));
            
        } catch (Exception e) {
            logger.error("Failed to migrate guest data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MigrationResponse.error("Migration failed: " + e.getMessage()));
        }
    }
    
    /**
     * Check if a user is a guest user.
     * GET /api/v1/users/is-guest
     */
    @GetMapping("/is-guest")
    public ResponseEntity<Map<String, Object>> isGuest(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        Boolean isGuest = (Boolean) httpRequest.getAttribute("isGuest");
        
        if (userId == null) {
            return ResponseEntity.ok(Map.of(
                "isGuest", true,
                "userId", "unknown"
            ));
        }
        
        boolean guestStatus = isGuest != null ? isGuest : GuestUserUtil.isGuestUser(userId);
        
        return ResponseEntity.ok(Map.of(
            "isGuest", guestStatus,
            "userId", userId
        ));
    }
    
    // ===== REQUEST/RESPONSE DTOs =====
    
    /**
     * Request DTO for guest data migration.
     */
    public static class MigrationRequest {
        private String guestUserId;
        
        public String getGuestUserId() {
            return guestUserId;
        }
        
        public void setGuestUserId(String guestUserId) {
            this.guestUserId = guestUserId;
        }
    }
    
    /**
     * Response DTO for guest data migration.
     */
    public static class MigrationResponse {
        private boolean success;
        private String message;
        private Integer migratedCount;
        
        public static MigrationResponse success(int migratedCount) {
            MigrationResponse response = new MigrationResponse();
            response.success = true;
            response.message = "Guest data migrated successfully";
            response.migratedCount = migratedCount;
            return response;
        }
        
        public static MigrationResponse error(String message) {
            MigrationResponse response = new MigrationResponse();
            response.success = false;
            response.message = message;
            response.migratedCount = 0;
            return response;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public Integer getMigratedCount() {
            return migratedCount;
        }
        
        public void setMigratedCount(Integer migratedCount) {
            this.migratedCount = migratedCount;
        }
    }
}
