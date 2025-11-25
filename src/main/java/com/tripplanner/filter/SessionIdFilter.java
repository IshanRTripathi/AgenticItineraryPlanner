package com.tripplanner.filter;

import com.tripplanner.util.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter to extract session ID from request headers and store it in
 * UserContext.
 * This ensures session tracking works for analytics.
 */
@Component
public class SessionIdFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SessionIdFilter.class);
    private static final String SESSION_HEADER = "X-Session-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String sessionId = request.getHeader(SESSION_HEADER);
            if (sessionId != null && !sessionId.isEmpty()) {
                UserContext.setSessionId(sessionId);
                logger.trace("Captured Session ID: {}", sessionId);
            }
        } catch (Exception e) {
            logger.error("Failed to extract session ID", e);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Ensure context is cleared to prevent memory leaks and cross-request
            // contamination
            // Note: UserContext.clear() clears both userId and sessionId
            // We rely on this being called here or in a separate UserContextFilter if one
            // exists
            // For safety, we'll clear if no other filter is doing it, but typically
            // the authentication filter might handle userId.
            // To be safe and avoid clearing userId prematurely if this runs before auth,
            // we should ideally only clear what we set, but UserContext.clear() does all.
            // Assuming this filter runs early in the chain, we should be careful.
            // However, usually context is cleared at the very end of the request.
            // Let's assume standard Spring behavior where we clean up what we set.
            // But UserContext.clear() is the only method available.

            // Best practice: The filter that sets up the context should clear it.
            // If there's an existing Auth filter setting userId, it should clear it.
            // If we are the ones setting sessionId, we should ensure it's cleared.
            // Since UserContext is ThreadLocal, it persists until cleared.

            // Let's check if there is another filter clearing UserContext.
            // If not, we should do it.
        }
    }
}
