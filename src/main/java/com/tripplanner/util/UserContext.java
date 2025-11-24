package com.tripplanner.util;

/**
 * Thread-local storage for user context.
 * Allows services deep in the call stack to access the current user ID.
 */
public class UserContext {

    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> sessionIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> agentNameHolder = new ThreadLocal<>();

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
     * Set the current session ID for this thread.
     */
    public static void setSessionId(String sessionId) {
        sessionIdHolder.set(sessionId);
    }

    /**
     * Get the current session ID for this thread.
     */
    public static String getSessionId() {
        return sessionIdHolder.get();
    }

    /**
     * Set the current agent name for this thread.
     */
    public static void setAgentName(String agentName) {
        agentNameHolder.set(agentName);
    }

    /**
     * Get the current agent name for this thread.
     */
    public static String getAgentName() {
        return agentNameHolder.get();
    }

    /**
     * Clear the user context for this thread.
     * Should be called after request processing to prevent memory leaks.
     */
    public static void clear() {
        userIdHolder.remove();
        sessionIdHolder.remove();
        agentNameHolder.remove();
    }

    /**
     * Check if a user is set in the current context.
     */
    public static boolean hasUser() {
        return userIdHolder.get() != null;
    }
}
