package com.tripplanner.service.analytics;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.gson.Gson;
import com.tripplanner.dto.CreateItineraryReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized service for tracking itinerary metrics to Pub/Sub → BigQuery.
 * Sends all 250+ metrics for SQL-based aggregation and analysis.
 */
@Service
public class ItineraryMetricsTracker {

    private static final Logger logger = LoggerFactory.getLogger(ItineraryMetricsTracker.class);

    @Autowired(required = false)
    private PubSubTemplate pubSubTemplate;

    private final Gson gson = new Gson();

    @Value("${analytics.pubsub.topic:analytics-events}")
    private String analyticsTopic;

    @Value("${analytics.enabled:true}")
    private boolean analyticsEnabled;

    /**
     * Track itinerary creation event.
     */
    public void trackItineraryCreated(String itineraryId, CreateItineraryReq request) {
        if (!analyticsEnabled || pubSubTemplate == null)
            return;

        try {
            int durationDays = request.getDurationDays();
            int partySize = request.getTotalPartySize();

            Map<String, Object> properties = new HashMap<>();
            properties.put("itineraryId", itineraryId);
            properties.put("destination", request.getDestination());
            properties.put("durationDays", durationDays);
            properties.put("partySize", partySize);
            properties.put("budgetTier", request.getBudgetTier());
            properties.put("budgetMin", request.getBudgetMin());
            properties.put("budgetMax", request.getBudgetMax());
            properties.put("interests", request.getInterests());
            if (request.getStartDate() != null) {
                properties.put("startDate", request.getStartDate().toString());
            }
            if (request.getEndDate() != null) {
                properties.put("endDate", request.getEndDate().toString());
            }

            publishEvent("itinerary_created", properties);
            logger.debug("Tracked itinerary creation: {}", itineraryId);
        } catch (Exception e) {
            logger.error("Failed to track itinerary creation", e);
        }
    }

    /**
     * Track pipeline phase completion.
     */
    public void trackPhaseCompleted(String itineraryId, String phase, long durationMs, boolean success,
            String errorMessage) {
        if (!analyticsEnabled || pubSubTemplate == null)
            return;

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("itineraryId", itineraryId);
            properties.put("phase", phase);
            properties.put("durationMs", durationMs);
            properties.put("success", success);
            if (errorMessage != null) {
                properties.put("errorMessage", errorMessage);
            }

            publishEvent("phase_completed", properties);
            logger.debug("Tracked phase completion: {} - {} ({}ms)", itineraryId, phase, durationMs);
        } catch (Exception e) {
            logger.error("Failed to track phase completion", e);
        }
    }

    /**
     * Track LLM request completion.
     */
    public void trackLLMRequest(String itineraryId, String agent, String model, String provider,
            int promptTokens, int responseTokens, int thoughtsTokens, int cachedTokens,
            int totalTokens, long durationMs, boolean success, String errorMessage) {
        if (!analyticsEnabled || pubSubTemplate == null)
            return;

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("itineraryId", itineraryId);
            properties.put("agent", agent);
            properties.put("model", model);
            properties.put("provider", provider);
            properties.put("promptTokens", promptTokens);
            properties.put("responseTokens", responseTokens);
            properties.put("thoughtsTokens", thoughtsTokens);
            properties.put("cachedTokens", cachedTokens);
            properties.put("totalTokens", totalTokens);
            properties.put("durationMs", durationMs);
            properties.put("success", success);
            if (errorMessage != null) {
                properties.put("errorMessage", errorMessage);
            }

            publishEvent("llm_request_completed", properties);
            logger.debug("Tracked LLM request: {} - {} ({}ms, {} tokens)", itineraryId, agent, durationMs, totalTokens);
        } catch (Exception e) {
            logger.error("Failed to track LLM request", e);
        }
    }

    /**
     * Track cost estimation for an activity.
     */
    public void trackCostEstimated(String itineraryId, String nodeId, String nodeType,
            double costPerPerson, String currency, int dayNumber, String activityName) {
        if (!analyticsEnabled || pubSubTemplate == null)
            return;

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("itineraryId", itineraryId);
            properties.put("nodeId", nodeId);
            properties.put("nodeType", nodeType);
            properties.put("costPerPerson", costPerPerson);
            properties.put("currency", currency);
            properties.put("dayNumber", dayNumber);
            properties.put("activityName", activityName);

            publishEvent("cost_estimated", properties);
            logger.debug("Tracked cost estimate: {} - {} = {} {}", itineraryId, nodeId, costPerPerson, currency);
        } catch (Exception e) {
            logger.error("Failed to track cost estimation", e);
        }
    }

    /**
     * Track validation completion.
     */
    /**
     * Track validation completion.
     */
    public void trackValidationCompleted(String itineraryId, int errorCount, int warningCount, boolean isValid,
            long durationMs) {
        if (!analyticsEnabled || pubSubTemplate == null)
            return;

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("itineraryId", itineraryId);
            properties.put("errorCount", errorCount);
            properties.put("warningCount", warningCount);
            properties.put("isValid", isValid);
            properties.put("durationMs", durationMs);

            publishEvent("validation_completed", properties);
            logger.debug("Tracked validation: {} (valid={}, errors={}, warnings={})",
                    itineraryId, isValid, errorCount, warningCount);
        } catch (Exception e) {
            logger.error("Failed to track validation", e);
        }
    }

    /**
     * Track itinerary completion.
     */
    public void trackItineraryCompleted(String itineraryId, long totalDurationMs, long totalActivities,
            double totalCost, String currency, int validationErrors,
            int validationWarnings, boolean success) {
        if (!analyticsEnabled || pubSubTemplate == null)
            return;

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("itineraryId", itineraryId);
            properties.put("totalDurationMs", totalDurationMs);
            properties.put("totalActivities", totalActivities);
            properties.put("totalCost", totalCost);
            properties.put("currency", currency);
            properties.put("validationErrors", validationErrors);
            properties.put("validationWarnings", validationWarnings);
            properties.put("success", success);

            publishEvent("itinerary_completed", properties);
            logger.info("Tracked itinerary completion: {} ({}ms, {} activities, {} {})",
                    itineraryId, totalDurationMs, totalActivities, totalCost, currency);
        } catch (Exception e) {
            logger.error("Failed to track itinerary completion", e);
        }
    }

    /**
     * Track validation warning.
     */
    public void trackValidationWarning(String itineraryId, String warningType, String nodeId,
            String message, String severity) {
        if (!analyticsEnabled || pubSubTemplate == null)
            return;

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("itineraryId", itineraryId);
            properties.put("warningType", warningType);
            if (nodeId != null) {
                properties.put("nodeId", nodeId);
            }
            properties.put("message", message);
            properties.put("severity", severity);

            publishEvent("validation_warning", properties);
            logger.debug("Tracked validation warning: {} - {} ({})", itineraryId, warningType, nodeId);
        } catch (Exception e) {
            logger.error("Failed to track validation warning", e);
        }
    }

    /**
     * Track API call completion (Google Places, etc.)
     */
    public void trackAPICall(String itineraryId, String apiProvider, String endpoint,
            long durationMs, boolean success, Integer statusCode, String errorMessage) {
        if (!analyticsEnabled || pubSubTemplate == null)
            return;

        try {
            Map<String, Object> properties = new HashMap<>();
            if (itineraryId != null) {
                properties.put("itineraryId", itineraryId);
            }
            properties.put("apiProvider", apiProvider);
            properties.put("endpoint", endpoint);
            properties.put("durationMs", durationMs);
            properties.put("success", success);
            if (statusCode != null) {
                properties.put("statusCode", statusCode);
            }
            if (errorMessage != null) {
                properties.put("errorMessage", errorMessage);
            }

            publishEvent("api_call_completed", properties);
            logger.debug("Tracked API call: {} - {} ({}ms)", apiProvider, endpoint, durationMs);
        } catch (Exception e) {
            logger.error("Failed to track API call", e);
        }
    }

    /**
     * Get current user ID from security context or UserContext.
     */
    private String getCurrentUserId() {
        // First try UserContext (for async threads where it was manually propagated)
        String userId = com.tripplanner.util.UserContext.getUserId();
        if (userId != null) {
            return userId;
        }

        // Fallback to SecurityContext (for synchronous requests)
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                return auth.getName();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Publish event to Pub/Sub.
     */
    private void publishEvent(String eventName, Map<String, Object> properties) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventName", eventName);
            event.put("timestamp", System.currentTimeMillis());
            event.put("userId", getCurrentUserId());
            event.put("sessionId", com.tripplanner.util.UserContext.getSessionId());
            event.put("platform", "backend");
            event.put("properties", properties);

            String eventJson = gson.toJson(event);
            pubSubTemplate.publish(analyticsTopic, eventJson);

            logger.trace("Published metric event: {} - {}", eventName, properties.get("itineraryId"));
        } catch (Exception e) {
            logger.error("Failed to publish metric event: {}", eventName, e);
            // Don't throw - metrics should never break the request
        }
    }
}
