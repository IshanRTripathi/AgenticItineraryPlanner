package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a durable agent task that can survive system restarts and be retried on failure.
 * Tasks are persisted in Firestore and processed by the AgentTaskSystem.
 */
public class AgentTask {
    
    @JsonProperty("taskId")
    private String taskId; // Unique task identifier
    
    @JsonProperty("idempotencyKey")
    private String idempotencyKey; // For duplicate detection
    
    @JsonProperty("taskType")
    private String taskType; // Type of task (e.g., "enrich", "plan", "book")
    
    @JsonProperty("agentKind")
    private String agentKind; // Target agent kind (e.g., "ENRICHMENT", "PLANNER")
    
    @JsonProperty("status")
    private TaskStatus status; // Current task status
    
    @JsonProperty("priority")
    private Integer priority; // Task priority (1-10, higher = more urgent)
    
    @JsonProperty("itineraryId")
    private String itineraryId; // Associated itinerary ID
    
    @JsonProperty("userId")
    private String userId; // User who owns this task
    
    @JsonProperty("payload")
    private Map<String, Object> payload; // Task-specific data
    
    @JsonProperty("result")
    private Map<String, Object> result; // Task execution result
    
    @JsonProperty("error")
    private TaskError error; // Error information if task failed
    
    @JsonProperty("retryConfig")
    private RetryConfig retryConfig; // Retry configuration
    
    @JsonProperty("attempts")
    private List<TaskAttempt> attempts; // Execution attempts history
    
    @JsonProperty("createdAt")
    private Long createdAt; // Task creation timestamp
    
    @JsonProperty("scheduledAt")
    private Long scheduledAt; // When task should be executed
    
    @JsonProperty("startedAt")
    private Long startedAt; // When task execution started
    
    @JsonProperty("completedAt")
    private Long completedAt; // When task completed (success or failure)
    
    @JsonProperty("updatedAt")
    private Long updatedAt; // Last update timestamp
    
    @JsonProperty("nextRetryTime")
    private Long nextRetryTime; // When task should be retried (if applicable)
    
    @JsonProperty("timeoutMs")
    private Long timeoutMs; // Task timeout in milliseconds
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata; // Additional metadata
    
    public AgentTask() {
        this.status = TaskStatus.PENDING;
        this.priority = 5; // Default medium priority
        this.payload = new HashMap<>();
        this.result = new HashMap<>();
        this.attempts = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.scheduledAt = System.currentTimeMillis();
        this.timeoutMs = 300000L; // Default 5 minutes timeout
        this.retryConfig = new RetryConfig();
    }
    
    public AgentTask(String taskId, String taskType, String agentKind, String itineraryId, String userId) {
        this();
        this.taskId = taskId;
        this.taskType = taskType;
        this.agentKind = agentKind;
        this.itineraryId = itineraryId;
        this.userId = userId;
    }
    
    /**
     * Mark task as started.
     */
    public void markAsStarted() {
        this.status = TaskStatus.RUNNING;
        this.startedAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * Mark task as completed successfully.
     */
    public void markAsCompleted(Map<String, Object> result) {
        this.status = TaskStatus.COMPLETED;
        this.result = result != null ? result : new HashMap<>();
        this.completedAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * Mark task as failed.
     */
    public void markAsFailed(String errorMessage, String errorCode, Throwable exception) {
        this.status = TaskStatus.FAILED;
        this.error = new TaskError(errorMessage, errorCode, exception);
        this.completedAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * Mark task as cancelled.
     */
    public void markAsCancelled(String reason) {
        this.status = TaskStatus.CANCELLED;
        this.error = new TaskError("Task cancelled: " + reason, "CANCELLED", null);
        this.completedAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * Add a task attempt record.
     */
    public void addAttempt(TaskAttempt attempt) {
        if (attempt != null) {
            this.attempts.add(attempt);
            this.updatedAt = System.currentTimeMillis();
        }
    }
    
    /**
     * Get the current attempt number (1-based).
     */
    public int getCurrentAttemptNumber() {
        return attempts.size() + 1;
    }
    
    /**
     * Check if task can be retried.
     */
    public boolean canRetry() {
        return status == TaskStatus.FAILED && 
               retryConfig != null && 
               getCurrentAttemptNumber() <= retryConfig.getMaxAttempts();
    }
    
    /**
     * Check if task is in a terminal state.
     */
    public boolean isTerminal() {
        return status == TaskStatus.COMPLETED || 
               status == TaskStatus.CANCELLED || 
               (status == TaskStatus.FAILED && !canRetry());
    }
    
    /**
     * Check if task has timed out.
     */
    public boolean hasTimedOut() {
        if (status != TaskStatus.RUNNING || startedAt == null) {
            return false;
        }
        
        long elapsedMs = System.currentTimeMillis() - startedAt;
        return elapsedMs > timeoutMs;
    }
    
    /**
     * Calculate next retry delay in milliseconds.
     */
    public long getNextRetryDelayMs() {
        if (retryConfig == null || !canRetry()) {
            return 0;
        }
        
        int attemptNumber = getCurrentAttemptNumber() - 1; // 0-based for calculation
        long baseDelayMs = retryConfig.getBaseDelayMs();
        double multiplier = retryConfig.getBackoffMultiplier();
        long maxDelayMs = retryConfig.getMaxDelayMs();
        
        // Exponential backoff with jitter
        long delay = (long) (baseDelayMs * Math.pow(multiplier, attemptNumber));
        delay = Math.min(delay, maxDelayMs);
        
        // Add jitter (±25%)
        double jitter = 0.25 * delay * (Math.random() * 2 - 1);
        delay += (long) jitter;
        
        return Math.max(delay, baseDelayMs);
    }
    
    /**
     * Schedule task for retry.
     */
    public void scheduleRetry() {
        if (canRetry()) {
            this.status = TaskStatus.PENDING;
            this.scheduledAt = System.currentTimeMillis() + getNextRetryDelayMs();
            this.startedAt = null;
            this.error = null;
            this.updatedAt = System.currentTimeMillis();
        }
    }
    
    /**
     * Increment retry count (for backward compatibility).
     */
    public void incrementRetryCount() {
        // This is handled by the attempts list, but we can add a dummy attempt
        // if needed for compatibility
        this.updatedAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getTaskType() {
        return taskType;
    }
    
    public void setTaskType(String taskType) {
        this.taskType = taskType;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getAgentKind() {
        return agentKind;
    }
    
    public void setAgentKind(String agentKind) {
        this.agentKind = agentKind;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Map<String, Object> getPayload() {
        return payload;
    }
    
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload != null ? payload : new HashMap<>();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Map<String, Object> getResult() {
        return result;
    }
    
    public void setResult(Map<String, Object> result) {
        this.result = result != null ? result : new HashMap<>();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public TaskError getError() {
        return error;
    }
    
    public void setError(TaskError error) {
        this.error = error;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public RetryConfig getRetryConfig() {
        return retryConfig;
    }
    
    public void setRetryConfig(RetryConfig retryConfig) {
        this.retryConfig = retryConfig;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public List<TaskAttempt> getAttempts() {
        return attempts;
    }
    
    public void setAttempts(List<TaskAttempt> attempts) {
        this.attempts = attempts != null ? attempts : new ArrayList<>();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getScheduledAt() {
        return scheduledAt;
    }
    
    public void setScheduledAt(Long scheduledAt) {
        this.scheduledAt = scheduledAt;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Long getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Long startedAt) {
        this.startedAt = startedAt;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Long getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getNextRetryTime() {
        return nextRetryTime;
    }
    
    public void setNextRetryTime(Long nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Long getTimeoutMs() {
        return timeoutMs;
    }
    
    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.updatedAt = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "AgentTask{" +
                "taskId='" + taskId + '\'' +
                ", taskType='" + taskType + '\'' +
                ", agentKind='" + agentKind + '\'' +
                ", status=" + status +
                ", priority=" + priority +
                ", itineraryId='" + itineraryId + '\'' +
                ", attempts=" + attempts.size() +
                ", canRetry=" + canRetry() +
                '}';
    }
    
    /**
     * Task status enumeration.
     */
    public enum TaskStatus {
        PENDING,    // Task is waiting to be executed
        RUNNING,    // Task is currently being executed
        COMPLETED,  // Task completed successfully
        FAILED,     // Task failed (may be retryable)
        CANCELLED   // Task was cancelled
    }
    
    /**
     * Task error information.
     */
    public static class TaskError {
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("stackTrace")
        private String stackTrace;
        
        @JsonProperty("timestamp")
        private Long timestamp;
        
        public TaskError() {
            this.timestamp = System.currentTimeMillis();
        }
        
        public TaskError(String message, String code, Throwable exception) {
            this();
            this.message = message;
            this.code = code;
            if (exception != null) {
                this.stackTrace = getStackTraceString(exception);
            }
        }
        
        private String getStackTraceString(Throwable exception) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            exception.printStackTrace(pw);
            return sw.toString();
        }
        
        // Getters and Setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getStackTrace() { return stackTrace; }
        public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
        
        @Override
        public String toString() {
            return "TaskError{" +
                    "message='" + message + '\'' +
                    ", code='" + code + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
    
    /**
     * Retry configuration for tasks.
     */
    public static class RetryConfig {
        @JsonProperty("maxAttempts")
        private Integer maxAttempts;
        
        @JsonProperty("baseDelayMs")
        private Long baseDelayMs;
        
        @JsonProperty("maxDelayMs")
        private Long maxDelayMs;
        
        @JsonProperty("backoffMultiplier")
        private Double backoffMultiplier;
        
        public RetryConfig() {
            this.maxAttempts = 3;
            this.baseDelayMs = 1000L; // 1 second
            this.maxDelayMs = 300000L; // 5 minutes
            this.backoffMultiplier = 2.0; // Double delay each retry
        }
        
        public RetryConfig(Integer maxAttempts, Long baseDelayMs, Long maxDelayMs, Double backoffMultiplier) {
            this.maxAttempts = maxAttempts;
            this.baseDelayMs = baseDelayMs;
            this.maxDelayMs = maxDelayMs;
            this.backoffMultiplier = backoffMultiplier;
        }
        
        // Getters and Setters
        public Integer getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }
        public Long getBaseDelayMs() { return baseDelayMs; }
        public void setBaseDelayMs(Long baseDelayMs) { this.baseDelayMs = baseDelayMs; }
        public Long getMaxDelayMs() { return maxDelayMs; }
        public void setMaxDelayMs(Long maxDelayMs) { this.maxDelayMs = maxDelayMs; }
        public Double getBackoffMultiplier() { return backoffMultiplier; }
        public void setBackoffMultiplier(Double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
        
        @Override
        public String toString() {
            return "RetryConfig{" +
                    "maxAttempts=" + maxAttempts +
                    ", baseDelayMs=" + baseDelayMs +
                    ", maxDelayMs=" + maxDelayMs +
                    ", backoffMultiplier=" + backoffMultiplier +
                    '}';
        }
    }
    
    /**
     * Individual task attempt record.
     */
    public static class TaskAttempt {
        @JsonProperty("attemptNumber")
        private Integer attemptNumber;
        
        @JsonProperty("startedAt")
        private Long startedAt;
        
        @JsonProperty("completedAt")
        private Long completedAt;
        
        @JsonProperty("durationMs")
        private Long durationMs;
        
        @JsonProperty("status")
        private TaskStatus status;
        
        @JsonProperty("error")
        private TaskError error;
        
        public TaskAttempt() {}
        
        public TaskAttempt(Integer attemptNumber) {
            this.attemptNumber = attemptNumber;
            this.startedAt = System.currentTimeMillis();
        }
        
        public void markCompleted(TaskStatus status, TaskError error) {
            this.completedAt = System.currentTimeMillis();
            this.durationMs = this.completedAt - this.startedAt;
            this.status = status;
            this.error = error;
        }
        
        // Getters and Setters
        public Integer getAttemptNumber() { return attemptNumber; }
        public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }
        public Long getStartedAt() { return startedAt; }
        public void setStartedAt(Long startedAt) { this.startedAt = startedAt; }
        public Long getCompletedAt() { return completedAt; }
        public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }
        public Long getDurationMs() { return durationMs; }
        public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
        public TaskStatus getStatus() { return status; }
        public void setStatus(TaskStatus status) { this.status = status; }
        public TaskError getError() { return error; }
        public void setError(TaskError error) { this.error = error; }
        
        @Override
        public String toString() {
            return "TaskAttempt{" +
                    "attemptNumber=" + attemptNumber +
                    ", status=" + status +
                    ", durationMs=" + durationMs +
                    '}';
        }
    }
}