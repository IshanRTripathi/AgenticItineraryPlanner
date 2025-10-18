package com.tripplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for visualizing and debugging distributed traces.
 * Provides tools for trace analysis and troubleshooting.
 */
@Service
public class TraceVisualizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TraceVisualizationService.class);
    
    private final TraceManager traceManager;
    
    // Store completed traces for analysis
    private final Map<String, CompletedTrace> completedTraces = new ConcurrentHashMap<>();
    private final Map<String, List<TraceEvent>> traceEvents = new ConcurrentHashMap<>();
    
    public TraceVisualizationService(TraceManager traceManager) {
        this.traceManager = traceManager;
    }
    
    /**
     * Record a trace event for visualization.
     * 
     * @param traceId The trace ID
     * @param event The trace event
     */
    public void recordTraceEvent(String traceId, TraceEvent event) {
        traceEvents.computeIfAbsent(traceId, k -> new ArrayList<>()).add(event);
        logger.debug("Recorded trace event: {} for trace: {}", event.getEventType(), traceId);
    }
    
    /**
     * Complete a trace and store it for analysis.
     * 
     * @param traceContext The completed trace context
     */
    public void completeTrace(TraceManager.TraceContext traceContext) {
        String traceId = traceContext.getTraceId();
        List<TraceEvent> events = traceEvents.getOrDefault(traceId, new ArrayList<>());
        
        CompletedTrace completedTrace = new CompletedTrace(traceContext, events);
        completedTraces.put(traceId, completedTrace);
        
        // Clean up events
        traceEvents.remove(traceId);
        
        logger.info("Completed trace: {} with {} events (duration: {}ms)", 
                   traceId, events.size(), traceContext.getDuration());
    }
    
    /**
     * Get a trace timeline for visualization.
     * 
     * @param traceId The trace ID
     * @return TraceTimeline containing the trace visualization data
     */
    public TraceTimeline getTraceTimeline(String traceId) {
        CompletedTrace trace = completedTraces.get(traceId);
        if (trace == null) {
            return null;
        }
        
        List<TimelineEvent> timelineEvents = trace.getEvents().stream()
                .map(event -> new TimelineEvent(
                    event.getTimestamp(),
                    event.getEventType(),
                    event.getOperation(),
                    event.getDuration(),
                    event.getMetadata()
                ))
                .sorted(Comparator.comparing(TimelineEvent::getTimestamp))
                .collect(Collectors.toList());
        
        return new TraceTimeline(traceId, trace.getContext().getDuration(), timelineEvents);
    }
    
    /**
     * Get trace statistics for monitoring.
     * 
     * @return TraceVisualizationStats containing analysis data
     */
    public TraceVisualizationStats getVisualizationStats() {
        int completedTraceCount = completedTraces.size();
        int activeEventCount = traceEvents.values().stream()
                .mapToInt(List::size)
                .sum();
        
        // Calculate average trace duration
        double avgDuration = completedTraces.values().stream()
                .mapToLong(trace -> trace.getContext().getDuration())
                .average()
                .orElse(0.0);
        
        // Find slowest traces
        List<String> slowestTraces = completedTraces.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(
                    e2.getValue().getContext().getDuration(),
                    e1.getValue().getContext().getDuration()
                ))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        return new TraceVisualizationStats(completedTraceCount, activeEventCount, avgDuration, slowestTraces);
    }
    
    /**
     * Generate a trace summary for debugging.
     * 
     * @param traceId The trace ID
     * @return String containing the trace summary
     */
    public String generateTraceSummary(String traceId) {
        CompletedTrace trace = completedTraces.get(traceId);
        if (trace == null) {
            return "Trace not found: " + traceId;
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("=== TRACE SUMMARY ===\n");
        summary.append("Trace ID: ").append(traceId).append("\n");
        summary.append("Operation: ").append(trace.getContext().getOperation()).append("\n");
        summary.append("Duration: ").append(trace.getContext().getDuration()).append("ms\n");
        summary.append("Events: ").append(trace.getEvents().size()).append("\n\n");
        
        summary.append("=== EVENT TIMELINE ===\n");
        trace.getEvents().stream()
                .sorted(Comparator.comparing(TraceEvent::getTimestamp))
                .forEach(event -> {
                    summary.append(String.format("[%d] %s - %s (%dms)\n",
                        event.getTimestamp() - trace.getContext().getStartTime(),
                        event.getEventType(),
                        event.getOperation(),
                        event.getDuration()
                    ));
                    
                    if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
                        event.getMetadata().forEach((key, value) -> 
                            summary.append("  ").append(key).append(": ").append(value).append("\n")
                        );
                    }
                });
        
        return summary.toString();
    }
    
    /**
     * Find traces by operation type.
     * 
     * @param operation The operation to search for
     * @return List of trace IDs matching the operation
     */
    public List<String> findTracesByOperation(String operation) {
        return completedTraces.entrySet().stream()
                .filter(entry -> operation.equals(entry.getValue().getContext().getOperation()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Find slow traces above a duration threshold.
     * 
     * @param thresholdMs The duration threshold in milliseconds
     * @return List of slow trace IDs
     */
    public List<String> findSlowTraces(long thresholdMs) {
        return completedTraces.entrySet().stream()
                .filter(entry -> entry.getValue().getContext().getDuration() > thresholdMs)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Clean up old completed traces to prevent memory leaks.
     * 
     * @param maxAge Maximum age in milliseconds
     */
    public void cleanupOldTraces(long maxAge) {
        long cutoffTime = System.currentTimeMillis() - maxAge;
        
        List<String> oldTraces = completedTraces.entrySet().stream()
                .filter(entry -> entry.getValue().getContext().getStartTime() < cutoffTime)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        oldTraces.forEach(completedTraces::remove);
        
        if (!oldTraces.isEmpty()) {
            logger.info("Cleaned up {} old traces", oldTraces.size());
        }
    }
    
    /**
     * Represents a trace event for visualization.
     */
    public static class TraceEvent {
        private final long timestamp;
        private final String eventType;
        private final String operation;
        private final long duration;
        private final Map<String, Object> metadata;
        
        public TraceEvent(String eventType, String operation, long duration) {
            this(eventType, operation, duration, new HashMap<>());
        }
        
        public TraceEvent(String eventType, String operation, long duration, Map<String, Object> metadata) {
            this.timestamp = System.currentTimeMillis();
            this.eventType = eventType;
            this.operation = operation;
            this.duration = duration;
            this.metadata = new HashMap<>(metadata);
        }
        
        // Getters
        public long getTimestamp() { return timestamp; }
        public String getEventType() { return eventType; }
        public String getOperation() { return operation; }
        public long getDuration() { return duration; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    /**
     * Represents a completed trace with all its events.
     */
    public static class CompletedTrace {
        private final TraceManager.TraceContext context;
        private final List<TraceEvent> events;
        
        public CompletedTrace(TraceManager.TraceContext context, List<TraceEvent> events) {
            this.context = context;
            this.events = new ArrayList<>(events);
        }
        
        public TraceManager.TraceContext getContext() { return context; }
        public List<TraceEvent> getEvents() { return events; }
    }
    
    /**
     * Represents a timeline event for visualization.
     */
    public static class TimelineEvent {
        private final long timestamp;
        private final String eventType;
        private final String operation;
        private final long duration;
        private final Map<String, Object> metadata;
        
        public TimelineEvent(long timestamp, String eventType, String operation, 
                           long duration, Map<String, Object> metadata) {
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.operation = operation;
            this.duration = duration;
            this.metadata = metadata;
        }
        
        // Getters
        public long getTimestamp() { return timestamp; }
        public String getEventType() { return eventType; }
        public String getOperation() { return operation; }
        public long getDuration() { return duration; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    /**
     * Represents a trace timeline for visualization.
     */
    public static class TraceTimeline {
        private final String traceId;
        private final long totalDuration;
        private final List<TimelineEvent> events;
        
        public TraceTimeline(String traceId, long totalDuration, List<TimelineEvent> events) {
            this.traceId = traceId;
            this.totalDuration = totalDuration;
            this.events = events;
        }
        
        // Getters
        public String getTraceId() { return traceId; }
        public long getTotalDuration() { return totalDuration; }
        public List<TimelineEvent> getEvents() { return events; }
    }
    
    /**
     * Statistics about trace visualization.
     */
    public static class TraceVisualizationStats {
        private final int completedTraceCount;
        private final int activeEventCount;
        private final double averageDuration;
        private final List<String> slowestTraces;
        
        public TraceVisualizationStats(int completedTraceCount, int activeEventCount, 
                                     double averageDuration, List<String> slowestTraces) {
            this.completedTraceCount = completedTraceCount;
            this.activeEventCount = activeEventCount;
            this.averageDuration = averageDuration;
            this.slowestTraces = slowestTraces;
        }
        
        // Getters
        public int getCompletedTraceCount() { return completedTraceCount; }
        public int getActiveEventCount() { return activeEventCount; }
        public double getAverageDuration() { return averageDuration; }
        public List<String> getSlowestTraces() { return slowestTraces; }
    }
}