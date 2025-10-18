package com.tripplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Comprehensive system metrics collection for performance monitoring.
 * Tracks task latency, success rates, throughput, and system health.
 */
@Service
public class SystemMetrics {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemMetrics.class);
    
    // Task metrics
    private final Map<String, TaskMetrics> taskMetrics = new ConcurrentHashMap<>();
    
    // Place registry metrics
    private final Map<String, PlaceMetrics> placeMetrics = new ConcurrentHashMap<>();
    
    // Conflict resolution metrics
    private final ConflictMetrics conflictMetrics = new ConflictMetrics();
    
    // System health metrics
    private final SystemHealthMetrics healthMetrics = new SystemHealthMetrics();
    
    // Alert thresholds
    private static final long HIGH_LATENCY_THRESHOLD_MS = 5000; // 5 seconds
    private static final double LOW_SUCCESS_RATE_THRESHOLD = 0.95; // 95%
    private static final long HIGH_ERROR_RATE_THRESHOLD = 10; // 10 errors per minute
    
    /**
     * Record task execution metrics.
     * 
     * @param taskType The type of task
     * @param agentKind The agent that executed the task
     * @param latencyMs The execution latency in milliseconds
     * @param success Whether the task was successful
     */
    public void recordTaskExecution(String taskType, String agentKind, long latencyMs, boolean success) {
        String key = taskType + ":" + agentKind;
        TaskMetrics metrics = taskMetrics.computeIfAbsent(key, k -> new TaskMetrics(taskType, agentKind));
        
        metrics.recordExecution(latencyMs, success);
        
        // Check for alerts
        if (latencyMs > HIGH_LATENCY_THRESHOLD_MS) {
            logger.warn("High latency detected for {}: {}ms", key, latencyMs);
        }
        
        if (!success) {
            logger.warn("Task failure recorded for {}", key);
        }
    }
    
    /**
     * Record place merge operation metrics.
     * 
     * @param operation The merge operation type
     * @param latencyMs The operation latency
     * @param placesProcessed Number of places processed
     * @param duplicatesFound Number of duplicates found
     */
    public void recordPlaceMerge(String operation, long latencyMs, int placesProcessed, int duplicatesFound) {
        String key = "place_merge:" + operation;
        PlaceMetrics metrics = placeMetrics.computeIfAbsent(key, k -> new PlaceMetrics(operation));
        
        metrics.recordMergeOperation(latencyMs, placesProcessed, duplicatesFound);
        
        logger.debug("Place merge recorded: {} processed {} places, found {} duplicates in {}ms", 
                    operation, placesProcessed, duplicatesFound, latencyMs);
    }
    
    /**
     * Record conflict resolution metrics.
     * 
     * @param conflictType The type of conflict
     * @param resolutionMethod How the conflict was resolved
     * @param latencyMs Time taken to resolve
     * @param success Whether resolution was successful
     */
    public void recordConflictResolution(String conflictType, String resolutionMethod, 
                                       long latencyMs, boolean success) {
        conflictMetrics.recordResolution(conflictType, resolutionMethod, latencyMs, success);
        
        if (!success) {
            logger.warn("Conflict resolution failed: {} using {}", conflictType, resolutionMethod);
        }
    }
    
    /**
     * Record system health metrics.
     * 
     * @param memoryUsageMB Current memory usage in MB
     * @param cpuUsagePercent Current CPU usage percentage
     * @param activeConnections Number of active connections
     */
    public void recordSystemHealth(long memoryUsageMB, double cpuUsagePercent, int activeConnections) {
        healthMetrics.recordHealth(memoryUsageMB, cpuUsagePercent, activeConnections);
    }
    
    /**
     * Get task metrics for a specific task type and agent.
     * 
     * @param taskType The task type
     * @param agentKind The agent kind
     * @return TaskMetrics or null if not found
     */
    public TaskMetrics getTaskMetrics(String taskType, String agentKind) {
        return taskMetrics.get(taskType + ":" + agentKind);
    }
    
    /**
     * Get all task metrics.
     * 
     * @return Map of all task metrics
     */
    public Map<String, TaskMetrics> getAllTaskMetrics() {
        return new HashMap<>(taskMetrics);
    }
    
    /**
     * Get place metrics for a specific operation.
     * 
     * @param operation The place operation
     * @return PlaceMetrics or null if not found
     */
    public PlaceMetrics getPlaceMetrics(String operation) {
        return placeMetrics.get("place_merge:" + operation);
    }
    
    /**
     * Get conflict resolution metrics.
     * 
     * @return ConflictMetrics
     */
    public ConflictMetrics getConflictMetrics() {
        return conflictMetrics;
    }
    
    /**
     * Get system health metrics.
     * 
     * @return SystemHealthMetrics
     */
    public SystemHealthMetrics getSystemHealthMetrics() {
        return healthMetrics;
    }
    
    /**
     * Get comprehensive system metrics summary.
     * 
     * @return MetricsSummary containing all metrics
     */
    public MetricsSummary getMetricsSummary() {
        return new MetricsSummary(
            new HashMap<>(taskMetrics),
            new HashMap<>(placeMetrics),
            conflictMetrics,
            healthMetrics
        );
    }
    
    /**
     * Check for metric-based alerts.
     * 
     * @return List of active alerts
     */
    public List<MetricAlert> checkAlerts() {
        List<MetricAlert> alerts = new ArrayList<>();
        
        // Check task success rates
        taskMetrics.forEach((key, metrics) -> {
            double successRate = metrics.getSuccessRate();
            if (successRate < LOW_SUCCESS_RATE_THRESHOLD) {
                alerts.add(new MetricAlert(
                    MetricAlert.AlertType.LOW_SUCCESS_RATE,
                    key,
                    "Low success rate: " + String.format("%.2f%%", successRate * 100),
                    MetricAlert.Severity.WARNING
                ));
            }
        });
        
        // Check system health
        if (healthMetrics.getCurrentMemoryUsageMB() > 1024) { // 1GB threshold
            alerts.add(new MetricAlert(
                MetricAlert.AlertType.HIGH_MEMORY_USAGE,
                "system",
                "High memory usage: " + healthMetrics.getCurrentMemoryUsageMB() + "MB",
                MetricAlert.Severity.WARNING
            ));
        }
        
        if (healthMetrics.getCurrentCpuUsagePercent() > 80.0) { // 80% threshold
            alerts.add(new MetricAlert(
                MetricAlert.AlertType.HIGH_CPU_USAGE,
                "system",
                "High CPU usage: " + String.format("%.1f%%", healthMetrics.getCurrentCpuUsagePercent()),
                MetricAlert.Severity.WARNING
            ));
        }
        
        return alerts;
    }
    
    /**
     * Reset all metrics (useful for testing).
     */
    public void resetMetrics() {
        taskMetrics.clear();
        placeMetrics.clear();
        conflictMetrics.reset();
        healthMetrics.reset();
        logger.info("All metrics have been reset");
    }
    
    /**
     * Periodic metrics logging and cleanup.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void logMetricsSummary() {
        logger.info("=== METRICS SUMMARY ===");
        logger.info("Task metrics: {} types tracked", taskMetrics.size());
        logger.info("Place metrics: {} operations tracked", placeMetrics.size());
        logger.info("Conflict resolutions: {} total", conflictMetrics.getTotalResolutions());
        logger.info("System health: {}MB memory, {:.1f}% CPU", 
                   healthMetrics.getCurrentMemoryUsageMB(), 
                   healthMetrics.getCurrentCpuUsagePercent());
        
        // Check and log alerts
        List<MetricAlert> alerts = checkAlerts();
        if (!alerts.isEmpty()) {
            logger.warn("Active alerts: {}", alerts.size());
            alerts.forEach(alert -> logger.warn("ALERT: {} - {}", alert.getType(), alert.getMessage()));
        }
    }
    
    /**
     * Task execution metrics.
     */
    public static class TaskMetrics {
        private final String taskType;
        private final String agentKind;
        private final LongAdder totalExecutions = new LongAdder();
        private final LongAdder successfulExecutions = new LongAdder();
        private final LongAdder totalLatencyMs = new LongAdder();
        private final AtomicLong minLatencyMs = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxLatencyMs = new AtomicLong(0);
        private final AtomicLong lastExecutionTime = new AtomicLong(0);
        
        public TaskMetrics(String taskType, String agentKind) {
            this.taskType = taskType;
            this.agentKind = agentKind;
        }
        
        public void recordExecution(long latencyMs, boolean success) {
            totalExecutions.increment();
            if (success) {
                successfulExecutions.increment();
            }
            
            totalLatencyMs.add(latencyMs);
            minLatencyMs.updateAndGet(current -> Math.min(current, latencyMs));
            maxLatencyMs.updateAndGet(current -> Math.max(current, latencyMs));
            lastExecutionTime.set(System.currentTimeMillis());
        }
        
        // Getters
        public String getTaskType() { return taskType; }
        public String getAgentKind() { return agentKind; }
        public long getTotalExecutions() { return totalExecutions.sum(); }
        public long getSuccessfulExecutions() { return successfulExecutions.sum(); }
        public double getSuccessRate() { 
            long total = getTotalExecutions();
            return total > 0 ? (double) getSuccessfulExecutions() / total : 0.0;
        }
        public double getAverageLatencyMs() {
            long total = getTotalExecutions();
            return total > 0 ? (double) totalLatencyMs.sum() / total : 0.0;
        }
        public long getMinLatencyMs() { 
            long min = minLatencyMs.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
        public long getMaxLatencyMs() { return maxLatencyMs.get(); }
        public long getLastExecutionTime() { return lastExecutionTime.get(); }
    }
    
    /**
     * Place operation metrics.
     */
    public static class PlaceMetrics {
        private final String operation;
        private final LongAdder totalOperations = new LongAdder();
        private final LongAdder totalPlacesProcessed = new LongAdder();
        private final LongAdder totalDuplicatesFound = new LongAdder();
        private final LongAdder totalLatencyMs = new LongAdder();
        
        public PlaceMetrics(String operation) {
            this.operation = operation;
        }
        
        public void recordMergeOperation(long latencyMs, int placesProcessed, int duplicatesFound) {
            totalOperations.increment();
            totalPlacesProcessed.add(placesProcessed);
            totalDuplicatesFound.add(duplicatesFound);
            totalLatencyMs.add(latencyMs);
        }
        
        // Getters
        public String getOperation() { return operation; }
        public long getTotalOperations() { return totalOperations.sum(); }
        public long getTotalPlacesProcessed() { return totalPlacesProcessed.sum(); }
        public long getTotalDuplicatesFound() { return totalDuplicatesFound.sum(); }
        public double getAverageLatencyMs() {
            long total = getTotalOperations();
            return total > 0 ? (double) totalLatencyMs.sum() / total : 0.0;
        }
        public double getDuplicateRate() {
            long processed = getTotalPlacesProcessed();
            return processed > 0 ? (double) getTotalDuplicatesFound() / processed : 0.0;
        }
    }
    
    /**
     * Conflict resolution metrics.
     */
    public static class ConflictMetrics {
        private final Map<String, LongAdder> conflictsByType = new ConcurrentHashMap<>();
        private final Map<String, LongAdder> resolutionsByMethod = new ConcurrentHashMap<>();
        private final LongAdder totalResolutions = new LongAdder();
        private final LongAdder successfulResolutions = new LongAdder();
        private final LongAdder totalLatencyMs = new LongAdder();
        
        public void recordResolution(String conflictType, String resolutionMethod, 
                                   long latencyMs, boolean success) {
            conflictsByType.computeIfAbsent(conflictType, k -> new LongAdder()).increment();
            resolutionsByMethod.computeIfAbsent(resolutionMethod, k -> new LongAdder()).increment();
            totalResolutions.increment();
            if (success) {
                successfulResolutions.increment();
            }
            totalLatencyMs.add(latencyMs);
        }
        
        public void reset() {
            conflictsByType.clear();
            resolutionsByMethod.clear();
            totalResolutions.reset();
            successfulResolutions.reset();
            totalLatencyMs.reset();
        }
        
        // Getters
        public Map<String, Long> getConflictsByType() {
            Map<String, Long> result = new HashMap<>();
            conflictsByType.forEach((k, v) -> result.put(k, v.sum()));
            return result;
        }
        public Map<String, Long> getResolutionsByMethod() {
            Map<String, Long> result = new HashMap<>();
            resolutionsByMethod.forEach((k, v) -> result.put(k, v.sum()));
            return result;
        }
        public long getTotalResolutions() { return totalResolutions.sum(); }
        public long getSuccessfulResolutions() { return successfulResolutions.sum(); }
        public double getSuccessRate() {
            long total = getTotalResolutions();
            return total > 0 ? (double) getSuccessfulResolutions() / total : 0.0;
        }
        public double getAverageLatencyMs() {
            long total = getTotalResolutions();
            return total > 0 ? (double) totalLatencyMs.sum() / total : 0.0;
        }
    }
    
    /**
     * System health metrics.
     */
    public static class SystemHealthMetrics {
        private final AtomicLong currentMemoryUsageMB = new AtomicLong(0);
        private final AtomicLong currentCpuUsagePercent = new AtomicLong(0);
        private final AtomicLong currentActiveConnections = new AtomicLong(0);
        private final AtomicLong lastHealthCheckTime = new AtomicLong(0);
        
        public void recordHealth(long memoryUsageMB, double cpuUsagePercent, int activeConnections) {
            currentMemoryUsageMB.set(memoryUsageMB);
            currentCpuUsagePercent.set((long) (cpuUsagePercent * 100)); // Store as integer percentage * 100
            currentActiveConnections.set(activeConnections);
            lastHealthCheckTime.set(System.currentTimeMillis());
        }
        
        public void reset() {
            currentMemoryUsageMB.set(0);
            currentCpuUsagePercent.set(0);
            currentActiveConnections.set(0);
            lastHealthCheckTime.set(0);
        }
        
        // Getters
        public long getCurrentMemoryUsageMB() { return currentMemoryUsageMB.get(); }
        public double getCurrentCpuUsagePercent() { return currentCpuUsagePercent.get() / 100.0; }
        public long getCurrentActiveConnections() { return currentActiveConnections.get(); }
        public long getLastHealthCheckTime() { return lastHealthCheckTime.get(); }
    }
    
    /**
     * Comprehensive metrics summary.
     */
    public static class MetricsSummary {
        private final Map<String, TaskMetrics> taskMetrics;
        private final Map<String, PlaceMetrics> placeMetrics;
        private final ConflictMetrics conflictMetrics;
        private final SystemHealthMetrics healthMetrics;
        
        public MetricsSummary(Map<String, TaskMetrics> taskMetrics,
                            Map<String, PlaceMetrics> placeMetrics,
                            ConflictMetrics conflictMetrics,
                            SystemHealthMetrics healthMetrics) {
            this.taskMetrics = taskMetrics;
            this.placeMetrics = placeMetrics;
            this.conflictMetrics = conflictMetrics;
            this.healthMetrics = healthMetrics;
        }
        
        // Getters
        public Map<String, TaskMetrics> getTaskMetrics() { return taskMetrics; }
        public Map<String, PlaceMetrics> getPlaceMetrics() { return placeMetrics; }
        public ConflictMetrics getConflictMetrics() { return conflictMetrics; }
        public SystemHealthMetrics getHealthMetrics() { return healthMetrics; }
    }
    
    /**
     * Metric-based alert.
     */
    public static class MetricAlert {
        public enum AlertType {
            HIGH_LATENCY, LOW_SUCCESS_RATE, HIGH_ERROR_RATE, 
            HIGH_MEMORY_USAGE, HIGH_CPU_USAGE, HIGH_CONFLICT_RATE
        }
        
        public enum Severity {
            INFO, WARNING, CRITICAL
        }
        
        private final AlertType type;
        private final String component;
        private final String message;
        private final Severity severity;
        private final long timestamp;
        
        public MetricAlert(AlertType type, String component, String message, Severity severity) {
            this.type = type;
            this.component = component;
            this.message = message;
            this.severity = severity;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters
        public AlertType getType() { return type; }
        public String getComponent() { return component; }
        public String getMessage() { return message; }
        public Severity getSeverity() { return severity; }
        public long getTimestamp() { return timestamp; }
    }
}