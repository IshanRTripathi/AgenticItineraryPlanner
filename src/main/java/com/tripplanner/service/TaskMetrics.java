package com.tripplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for collecting and reporting metrics about agent task processing.
 * Provides insights into task performance, success rates, and system health.
 */
@Service
public class TaskMetrics {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskMetrics.class);
    
    private final com.tripplanner.service.SystemMetrics systemMetricsService;
    
    // Task counters
    private final AtomicLong totalTasksSubmitted = new AtomicLong(0);
    private final AtomicLong totalTasksStarted = new AtomicLong(0);
    private final AtomicLong totalTasksCompleted = new AtomicLong(0);
    private final AtomicLong totalTasksFailed = new AtomicLong(0);
    private final AtomicInteger currentlyRunningTasks = new AtomicInteger(0);
    
    // Task type metrics
    private final Map<String, TaskTypeMetrics> taskTypeMetrics = new ConcurrentHashMap<>();
    
    // Agent kind metrics
    private final Map<String, AgentKindMetrics> agentKindMetrics = new ConcurrentHashMap<>();
    
    // Timing metrics
    private final AtomicLong totalExecutionTimeMs = new AtomicLong(0);
    private final AtomicLong minExecutionTimeMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxExecutionTimeMs = new AtomicLong(0);
    
    // Error tracking
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    
    public TaskMetrics(com.tripplanner.service.SystemMetrics systemMetricsService) {
        this.systemMetricsService = systemMetricsService;
    }
    
    /**
     * Record that a task was submitted to the system.
     */
    public void recordTaskSubmitted(String taskType, String agentKind) {
        totalTasksSubmitted.incrementAndGet();
        getTaskTypeMetrics(taskType).submitted.incrementAndGet();
        getAgentKindMetrics(agentKind).submitted.incrementAndGet();
        
        logger.debug("Task submitted - Type: {}, Agent: {}, Total: {}", 
                    taskType, agentKind, totalTasksSubmitted.get());
    }
    
    /**
     * Record that a task started execution.
     */
    public void recordTaskStarted(String taskType, String agentKind) {
        totalTasksStarted.incrementAndGet();
        currentlyRunningTasks.incrementAndGet();
        getTaskTypeMetrics(taskType).started.incrementAndGet();
        getAgentKindMetrics(agentKind).started.incrementAndGet();
        
        logger.debug("Task started - Type: {}, Agent: {}, Running: {}", 
                    taskType, agentKind, currentlyRunningTasks.get());
    }
    
    /**
     * Record that a task completed (successfully or with failure).
     */
    public void recordTaskCompleted(String taskType, String agentKind, Long durationMs, boolean success) {
        currentlyRunningTasks.decrementAndGet();
        
        // Record in SystemMetrics for comprehensive monitoring
        if (durationMs != null) {
            systemMetricsService.recordTaskExecution(taskType, agentKind, durationMs.longValue(), success);
        }
        
        TaskTypeMetrics typeMetrics = getTaskTypeMetrics(taskType);
        AgentKindMetrics kindMetrics = getAgentKindMetrics(agentKind);
        
        if (success) {
            totalTasksCompleted.incrementAndGet();
            typeMetrics.completed.incrementAndGet();
            kindMetrics.completed.incrementAndGet();
        } else {
            totalTasksFailed.incrementAndGet();
            typeMetrics.failed.incrementAndGet();
            kindMetrics.failed.incrementAndGet();
        }
        
        // Update timing metrics
        if (durationMs != null && durationMs > 0) {
            totalExecutionTimeMs.addAndGet(durationMs);
            typeMetrics.totalExecutionTimeMs.addAndGet(durationMs);
            kindMetrics.totalExecutionTimeMs.addAndGet(durationMs);
            
            // Update min/max
            updateMinExecutionTime(durationMs);
            updateMaxExecutionTime(durationMs);
            typeMetrics.updateMinMaxExecutionTime(durationMs);
            kindMetrics.updateMinMaxExecutionTime(durationMs);
        }
        
        logger.debug("Task completed - Type: {}, Agent: {}, Success: {}, Duration: {}ms", 
                    taskType, agentKind, success, durationMs);
    }
    
    /**
     * Record a task error.
     */
    public void recordTaskError(String taskType, String agentKind, String errorCode) {
        String errorKey = taskType + ":" + errorCode;
        errorCounts.computeIfAbsent(errorKey, k -> new AtomicLong(0)).incrementAndGet();
        
        getTaskTypeMetrics(taskType).errorCounts.computeIfAbsent(errorCode, k -> new AtomicLong(0)).incrementAndGet();
        getAgentKindMetrics(agentKind).errorCounts.computeIfAbsent(errorCode, k -> new AtomicLong(0)).incrementAndGet();
        
        logger.debug("Task error recorded - Type: {}, Agent: {}, Error: {}", 
                    taskType, agentKind, errorCode);
    }
    
    /**
     * Record a task retry.
     */
    public void recordTaskRetry(String taskType, String agentKind, int attemptNumber) {
        getTaskTypeMetrics(taskType).retries.incrementAndGet();
        getAgentKindMetrics(agentKind).retries.incrementAndGet();
        
        logger.debug("Task retry recorded - Type: {}, Agent: {}, Attempt: {}", 
                    taskType, agentKind, attemptNumber);
    }
    
    /**
     * Get overall system metrics.
     */
    public SystemMetrics getSystemMetrics() {
        long totalCompleted = totalTasksCompleted.get();
        long totalFailed = totalTasksFailed.get();
        long totalFinished = totalCompleted + totalFailed;
        
        double successRate = totalFinished > 0 ? (double) totalCompleted / totalFinished : 0.0;
        double averageExecutionTimeMs = totalFinished > 0 ? 
            (double) totalExecutionTimeMs.get() / totalFinished : 0.0;
        
        return new SystemMetrics(
            totalTasksSubmitted.get(),
            totalTasksStarted.get(),
            totalCompleted,
            totalFailed,
            currentlyRunningTasks.get(),
            successRate,
            averageExecutionTimeMs,
            minExecutionTimeMs.get() == Long.MAX_VALUE ? 0 : minExecutionTimeMs.get(),
            maxExecutionTimeMs.get()
        );
    }
    
    /**
     * Get metrics for a specific task type.
     */
    public TaskTypeMetrics getTaskTypeMetrics(String taskType) {
        return taskTypeMetrics.computeIfAbsent(taskType, k -> new TaskTypeMetrics());
    }
    
    /**
     * Get metrics for a specific agent kind.
     */
    public AgentKindMetrics getAgentKindMetrics(String agentKind) {
        return agentKindMetrics.computeIfAbsent(agentKind, k -> new AgentKindMetrics());
    }
    
    /**
     * Get error statistics.
     */
    public Map<String, Long> getErrorStatistics() {
        Map<String, Long> stats = new ConcurrentHashMap<>();
        errorCounts.forEach((key, count) -> stats.put(key, count.get()));
        return stats;
    }
    
    /**
     * Reset all metrics (useful for testing).
     */
    public void reset() {
        totalTasksSubmitted.set(0);
        totalTasksStarted.set(0);
        totalTasksCompleted.set(0);
        totalTasksFailed.set(0);
        currentlyRunningTasks.set(0);
        totalExecutionTimeMs.set(0);
        minExecutionTimeMs.set(Long.MAX_VALUE);
        maxExecutionTimeMs.set(0);
        
        taskTypeMetrics.clear();
        agentKindMetrics.clear();
        errorCounts.clear();
        
        logger.info("Task metrics reset");
    }
    
    private void updateMinExecutionTime(long durationMs) {
        long currentMin = minExecutionTimeMs.get();
        while (durationMs < currentMin && !minExecutionTimeMs.compareAndSet(currentMin, durationMs)) {
            currentMin = minExecutionTimeMs.get();
        }
    }
    
    private void updateMaxExecutionTime(long durationMs) {
        long currentMax = maxExecutionTimeMs.get();
        while (durationMs > currentMax && !maxExecutionTimeMs.compareAndSet(currentMax, durationMs)) {
            currentMax = maxExecutionTimeMs.get();
        }
    }
    
    /**
     * Overall system metrics.
     */
    public static class SystemMetrics {
        private final long totalSubmitted;
        private final long totalStarted;
        private final long totalCompleted;
        private final long totalFailed;
        private final int currentlyRunning;
        private final double successRate;
        private final double averageExecutionTimeMs;
        private final long minExecutionTimeMs;
        private final long maxExecutionTimeMs;
        
        public SystemMetrics(long totalSubmitted, long totalStarted, long totalCompleted, 
                           long totalFailed, int currentlyRunning, double successRate,
                           double averageExecutionTimeMs, long minExecutionTimeMs, long maxExecutionTimeMs) {
            this.totalSubmitted = totalSubmitted;
            this.totalStarted = totalStarted;
            this.totalCompleted = totalCompleted;
            this.totalFailed = totalFailed;
            this.currentlyRunning = currentlyRunning;
            this.successRate = successRate;
            this.averageExecutionTimeMs = averageExecutionTimeMs;
            this.minExecutionTimeMs = minExecutionTimeMs;
            this.maxExecutionTimeMs = maxExecutionTimeMs;
        }
        
        // Getters
        public long getTotalSubmitted() { return totalSubmitted; }
        public long getTotalStarted() { return totalStarted; }
        public long getTotalCompleted() { return totalCompleted; }
        public long getTotalFailed() { return totalFailed; }
        public int getCurrentlyRunning() { return currentlyRunning; }
        public double getSuccessRate() { return successRate; }
        public double getAverageExecutionTimeMs() { return averageExecutionTimeMs; }
        public long getMinExecutionTimeMs() { return minExecutionTimeMs; }
        public long getMaxExecutionTimeMs() { return maxExecutionTimeMs; }
        
        @Override
        public String toString() {
            return String.format("SystemMetrics{submitted=%d, started=%d, completed=%d, failed=%d, " +
                               "running=%d, successRate=%.2f%%, avgTime=%.1fms, minTime=%dms, maxTime=%dms}",
                               totalSubmitted, totalStarted, totalCompleted, totalFailed,
                               currentlyRunning, successRate * 100, averageExecutionTimeMs,
                               minExecutionTimeMs, maxExecutionTimeMs);
        }
    }
    
    /**
     * Metrics for a specific task type.
     */
    public static class TaskTypeMetrics {
        public final AtomicLong submitted = new AtomicLong(0);
        public final AtomicLong started = new AtomicLong(0);
        public final AtomicLong completed = new AtomicLong(0);
        public final AtomicLong failed = new AtomicLong(0);
        public final AtomicLong retries = new AtomicLong(0);
        public final AtomicLong totalExecutionTimeMs = new AtomicLong(0);
        public final AtomicLong minExecutionTimeMs = new AtomicLong(Long.MAX_VALUE);
        public final AtomicLong maxExecutionTimeMs = new AtomicLong(0);
        public final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
        
        public void updateMinMaxExecutionTime(long durationMs) {
            // Update min
            long currentMin = minExecutionTimeMs.get();
            while (durationMs < currentMin && !minExecutionTimeMs.compareAndSet(currentMin, durationMs)) {
                currentMin = minExecutionTimeMs.get();
            }
            
            // Update max
            long currentMax = maxExecutionTimeMs.get();
            while (durationMs > currentMax && !maxExecutionTimeMs.compareAndSet(currentMax, durationMs)) {
                currentMax = maxExecutionTimeMs.get();
            }
        }
        
        public double getSuccessRate() {
            long totalFinished = completed.get() + failed.get();
            return totalFinished > 0 ? (double) completed.get() / totalFinished : 0.0;
        }
        
        public double getAverageExecutionTimeMs() {
            long totalFinished = completed.get() + failed.get();
            return totalFinished > 0 ? (double) totalExecutionTimeMs.get() / totalFinished : 0.0;
        }
        
        // Getters
        public long getSubmitted() { return submitted.get(); }
        public long getStarted() { return started.get(); }
        public long getCompleted() { return completed.get(); }
        public long getFailed() { return failed.get(); }
        public long getRetries() { return retries.get(); }
        public long getTotalExecutionTimeMs() { return totalExecutionTimeMs.get(); }
        public long getMinExecutionTimeMs() { 
            return minExecutionTimeMs.get() == Long.MAX_VALUE ? 0 : minExecutionTimeMs.get(); 
        }
        public long getMaxExecutionTimeMs() { return maxExecutionTimeMs.get(); }
        public Map<String, Long> getErrorCounts() {
            Map<String, Long> result = new ConcurrentHashMap<>();
            errorCounts.forEach((key, count) -> result.put(key, count.get()));
            return result;
        }
        
        @Override
        public String toString() {
            return String.format("TaskTypeMetrics{submitted=%d, completed=%d, failed=%d, " +
                               "successRate=%.2f%%, avgTime=%.1fms}",
                               submitted.get(), completed.get(), failed.get(),
                               getSuccessRate() * 100, getAverageExecutionTimeMs());
        }
    }
    
    /**
     * Metrics for a specific agent kind.
     */
    public static class AgentKindMetrics extends TaskTypeMetrics {
        // Inherits all functionality from TaskTypeMetrics
        // Could add agent-specific metrics here if needed
    }
}