package com.tripplanner.util;

/**
 * Thread-local storage for user context.
 * Allows services deep in the call stack to access the current user ID.
 */
public class UserContext {
    
    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();
    
    /**
     * Set the current user ID for this thread.
     */
    public static void setUserId(String userId) {
        userIdHolder.set(userId);
    }
    
    /**
     * Get the current user ID for this thread.
     * Returns null if no user is set (anonymous/guest).
     */
    public static String getUserId() {
        return userIdHolder.get();
    }
    
    /**
     * Clear the user context for this thread.
     * Should be called after request processing to prevent memory leaks.
     */
    public static void clear() {
        userIdHolder.remove();
    }
    
    /**
     * Check if a user is set in the current context.
     */
    public static boolean hasUser() {
        return userIdHolder.get() != null;
    }
}
