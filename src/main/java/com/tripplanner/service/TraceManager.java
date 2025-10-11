package com.tripplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages distributed tracing for end-to-end request correlation.
 * Provides traceId propagation across all operations and components.
 */
@Service
public class TraceManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TraceManager.class);
    
    // MDC keys for trace context
    public static final String TRACE_ID_KEY = "traceId";
    public static final String SPAN_ID_KEY = "spanId";
    public static final String PARENT_SPAN_ID_KEY = "parentSpanId";
    public static final String OPERATION_KEY = "operation";
    public static final String USER_ID_KEY = "userId";
    public static final String ITINERARY_ID_KEY = "itineraryId";
    
    // Active traces for monitoring
    private final Map<String, TraceContext> activeTraces = new ConcurrentHashMap<>();
    
    /**
     * Start a new trace with a generated trace ID.
     * 
     * @param operation The operation being traced
     * @return The generated trace ID
     */
    public String startTrace(String operation) {
        String traceId = generateTraceId();
        return startTrace(traceId, operation);
    }
    
    /**
     * Start a new trace with a specific trace ID.
     * 
     * @param traceId The trace ID to use
     * @param operation The operation being traced
     * @return The trace ID
     */
    public String startTrace(String traceId, String operation) {
        String spanId = generateSpanId();
        
        TraceContext context = new TraceContext(traceId, spanId, null, operation);
        activeTraces.put(traceId, context);
        
        // Set MDC context
        MDC.put(TRACE_ID_KEY, traceId);
        MDC.put(SPAN_ID_KEY, spanId);
        MDC.put(OPERATION_KEY, operation);
        
        logger.info("Started trace: {} for operation: {}", traceId, operation);
        return traceId;
    }
    
    /**
     * Start a child span within an existing trace.
     * 
     * @param operation The child operation being traced
     * @return The generated span ID
     */
    public String startSpan(String operation) {
        String traceId = getCurrentTraceId();
        String parentSpanId = getCurrentSpanId();
        String spanId = generateSpanId();
        
        if (traceId == null) {
            logger.warn("No active trace found, starting new trace for operation: {}", operation);
            return startTrace(operation);
        }
        
        // Update MDC context
        MDC.put(SPAN_ID_KEY, spanId);
        MDC.put(PARENT_SPAN_ID_KEY, parentSpanId);
        MDC.put(OPERATION_KEY, operation);
        
        logger.debug("Started span: {} (parent: {}) for operation: {}", spanId, parentSpanId, operation);
        return spanId;
    }
    
    /**
     * End the current span and restore parent context.
     * 
     * @param spanId The span ID to end
     */
    public void endSpan(String spanId) {
        String currentSpanId = getCurrentSpanId();
        if (!spanId.equals(currentSpanId)) {
            logger.warn("Ending span {} but current span is {}", spanId, currentSpanId);
        }
        
        String parentSpanId = MDC.get(PARENT_SPAN_ID_KEY);
        if (parentSpanId != null) {
            // Restore parent span context
            MDC.put(SPAN_ID_KEY, parentSpanId);
            MDC.remove(PARENT_SPAN_ID_KEY);
        }
        
        logger.debug("Ended span: {}", spanId);
    }
    
    /**
     * End the current trace and clean up context.
     * 
     * @param traceId The trace ID to end
     */
    public void endTrace(String traceId) {
        TraceContext context = activeTraces.remove(traceId);
        if (context != null) {
            context.setEndTime(System.currentTimeMillis());
            long duration = context.getDuration();
            logger.info("Ended trace: {} (duration: {}ms)", traceId, duration);
        }
        
        // Clear MDC context
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SPAN_ID_KEY);
        MDC.remove(PARENT_SPAN_ID_KEY);
        MDC.remove(OPERATION_KEY);
        MDC.remove(USER_ID_KEY);
        MDC.remove(ITINERARY_ID_KEY);
    }
    
    /**
     * Set user context for the current trace.
     * 
     * @param userId The user ID
     */
    public void setUserContext(String userId) {
        if (userId != null) {
            MDC.put(USER_ID_KEY, userId);
        }
    }
    
    /**
     * Set itinerary context for the current trace.
     * 
     * @param itineraryId The itinerary ID
     */
    public void setItineraryContext(String itineraryId) {
        if (itineraryId != null) {
            MDC.put(ITINERARY_ID_KEY, itineraryId);
        }
    }
    
    /**
     * Get the current trace ID from MDC.
     * 
     * @return The current trace ID or null if no trace is active
     */
    public String getCurrentTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }
    
    /**
     * Get the current span ID from MDC.
     * 
     * @return The current span ID or null if no span is active
     */
    public String getCurrentSpanId() {
        return MDC.get(SPAN_ID_KEY);
    }
    
    /**
     * Get the current trace context.
     * 
     * @return The current trace context or null if no trace is active
     */
    public TraceContext getCurrentTraceContext() {
        String traceId = getCurrentTraceId();
        return traceId != null ? activeTraces.get(traceId) : null;
    }
    
    /**
     * Copy trace context from another thread or request.
     * 
     * @param traceContext The trace context to copy
     */
    public void copyTraceContext(Map<String, String> traceContext) {
        if (traceContext != null) {
            traceContext.forEach(MDC::put);
        }
    }
    
    /**
     * Get the current trace context as a map for propagation.
     * 
     * @return Map containing trace context
     */
    public Map<String, String> getTraceContextMap() {
        Map<String, String> context = new ConcurrentHashMap<>();
        
        String traceId = getCurrentTraceId();
        String spanId = getCurrentSpanId();
        String operation = MDC.get(OPERATION_KEY);
        String userId = MDC.get(USER_ID_KEY);
        String itineraryId = MDC.get(ITINERARY_ID_KEY);
        
        if (traceId != null) context.put(TRACE_ID_KEY, traceId);
        if (spanId != null) context.put(SPAN_ID_KEY, spanId);
        if (operation != null) context.put(OPERATION_KEY, operation);
        if (userId != null) context.put(USER_ID_KEY, userId);
        if (itineraryId != null) context.put(ITINERARY_ID_KEY, itineraryId);
        
        return context;
    }
    
    /**
     * Execute a traced operation.
     * 
     * @param operation The operation name
     * @param tracedOperation The operation to execute
     * @return The result of the operation
     */
    public <T> T executeTraced(String operation, TracedOperation<T> tracedOperation) throws Exception {
        String spanId = startSpan(operation);
        try {
            return tracedOperation.execute();
        } catch (Exception e) {
            logger.error("Error in traced operation: {}", operation, e);
            throw e;
        } finally {
            endSpan(spanId);
        }
    }
    
    /**
     * Get statistics about active traces.
     * 
     * @return TraceStatistics containing current trace information
     */
    public TraceStatistics getTraceStatistics() {
        int activeTraceCount = activeTraces.size();
        long oldestTraceAge = 0;
        
        if (!activeTraces.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            oldestTraceAge = activeTraces.values().stream()
                    .mapToLong(context -> currentTime - context.getStartTime())
                    .max()
                    .orElse(0);
        }
        
        return new TraceStatistics(activeTraceCount, oldestTraceAge);
    }
    
    /**
     * Generate a unique trace ID.
     */
    private String generateTraceId() {
        return "trace_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Generate a unique span ID.
     */
    private String generateSpanId() {
        return "span_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * Functional interface for traced operations.
     */
    @FunctionalInterface
    public interface TracedOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * Represents a trace context with timing information.
     */
    public static class TraceContext {
        private final String traceId;
        private final String spanId;
        private final String parentSpanId;
        private final String operation;
        private final long startTime;
        private long endTime;
        
        public TraceContext(String traceId, String spanId, String parentSpanId, String operation) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.parentSpanId = parentSpanId;
            this.operation = operation;
            this.startTime = System.currentTimeMillis();
        }
        
        public String getTraceId() { return traceId; }
        public String getSpanId() { return spanId; }
        public String getParentSpanId() { return parentSpanId; }
        public String getOperation() { return operation; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        
        public long getDuration() {
            return endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime;
        }
    }
    
    /**
     * Statistics about trace activity.
     */
    public static class TraceStatistics {
        private final int activeTraceCount;
        private final long oldestTraceAgeMs;
        
        public TraceStatistics(int activeTraceCount, long oldestTraceAgeMs) {
            this.activeTraceCount = activeTraceCount;
            this.oldestTraceAgeMs = oldestTraceAgeMs;
        }
        
        public int getActiveTraceCount() { return activeTraceCount; }
        public long getOldestTraceAgeMs() { return oldestTraceAgeMs; }
    }
}