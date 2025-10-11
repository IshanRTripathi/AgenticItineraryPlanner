package com.tripplanner.service;

import com.google.cloud.firestore.*;
import com.tripplanner.dto.AgentTask;
import com.tripplanner.dto.AgentTask.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Manages the complete lifecycle of agent tasks including validation, monitoring,
 * timeout handling, and automatic recovery mechanisms.
 */
@Service
public class TaskLifecycleManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskLifecycleManager.class);
    
    @Autowired
    private Firestore firestore;
    
    private final TaskMetrics taskMetrics;
    private final AgentTaskSystem agentTaskSystem;
    
    // Task monitoring
    private final Map<String, TaskMonitor> activeTaskMonitors = new ConcurrentHashMap<>();
    
    // Configuration
    private static final String TASKS_COLLECTION = "agent_tasks";
    private static final String DEAD_LETTER_COLLECTION = "dead_letter_tasks";
    private static final long TASK_MONITOR_INTERVAL_MS = 30000; // 30 seconds
    private static final long STALE_TASK_THRESHOLD_MS = 600000; // 10 minutes
    private static final long ZOMBIE_TASK_THRESHOLD_MS = 1800000; // 30 minutes
    
    public TaskLifecycleManager(TaskMetrics taskMetrics, @org.springframework.context.annotation.Lazy AgentTaskSystem agentTaskSystem) {
        this.taskMetrics = taskMetrics;
        this.agentTaskSystem = agentTaskSystem;
    }
    
    @PostConstruct
    public void initialize() {
        logger.info("Initializing Task Lifecycle Manager");
        
        // Start monitoring existing running tasks
        monitorExistingRunningTasks();
        
        logger.info("Task Lifecycle Manager initialized");
    }
    
    /**
     * Validate task submission and apply business rules.
     */
    public TaskValidationResult validateTaskSubmission(AgentTask task) {
        logger.debug("Validating task submission: {}", task.getTaskId());
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Basic validation
        if (task.getTaskId() == null || task.getTaskId().trim().isEmpty()) {
            errors.add("Task ID is required");
        }
        
        if (task.getTaskType() == null || task.getTaskType().trim().isEmpty()) {
            errors.add("Task type is required");
        }
        
        if (task.getAgentKind() == null || task.getAgentKind().trim().isEmpty()) {
            errors.add("Agent kind is required");
        }
        
        if (task.getItineraryId() == null || task.getItineraryId().trim().isEmpty()) {
            errors.add("Itinerary ID is required");
        }
        
        if (task.getUserId() == null || task.getUserId().trim().isEmpty()) {
            errors.add("User ID is required");
        }
        
        // Business rule validation
        if (task.getPriority() != null && (task.getPriority() < 1 || task.getPriority() > 10)) {
            warnings.add("Priority should be between 1 and 10, using default");
            task.setPriority(5);
        }
        
        if (task.getTimeoutMs() != null && task.getTimeoutMs() < 1000) {
            warnings.add("Timeout too short, using minimum of 1 second");
            task.setTimeoutMs(1000L);
        }
        
        if (task.getTimeoutMs() != null && task.getTimeoutMs() > 3600000) {
            warnings.add("Timeout too long, using maximum of 1 hour");
            task.setTimeoutMs(3600000L);
        }
        
        // Check for duplicate idempotency key
        if (task.getIdempotencyKey() != null) {
            try {
                if (isDuplicateIdempotencyKey(task.getIdempotencyKey())) {
                    warnings.add("Duplicate idempotency key detected");
                }
            } catch (Exception e) {
                logger.warn("Failed to check idempotency key: {}", task.getIdempotencyKey(), e);
            }
        }
        
        // Validate retry configuration
        if (task.getRetryConfig() != null) {
            validateRetryConfig(task.getRetryConfig(), warnings);
        }
        
        boolean isValid = errors.isEmpty();
        
        logger.debug("Task validation result for {}: valid={}, errors={}, warnings={}", 
                    task.getTaskId(), isValid, errors.size(), warnings.size());
        
        return new TaskValidationResult(isValid, errors, warnings);
    }
    
    /**
     * Start monitoring a task for timeout and health checks.
     */
    public void startTaskMonitoring(AgentTask task) {
        if (task.getStatus() == TaskStatus.RUNNING) {
            TaskMonitor monitor = new TaskMonitor(task.getTaskId(), task.getTimeoutMs());
            activeTaskMonitors.put(task.getTaskId(), monitor);
            
            logger.debug("Started monitoring task: {} with timeout: {}ms", 
                        task.getTaskId(), task.getTimeoutMs());
        }
    }
    
    /**
     * Stop monitoring a task.
     */
    public void stopTaskMonitoring(String taskId) {
        TaskMonitor monitor = activeTaskMonitors.remove(taskId);
        if (monitor != null) {
            logger.debug("Stopped monitoring task: {}", taskId);
        }
    }
    
    /**
     * Handle task completion and cleanup.
     */
    public void handleTaskCompletion(AgentTask task) {
        logger.debug("Handling task completion: {} (status: {})", task.getTaskId(), task.getStatus());
        
        // Stop monitoring
        stopTaskMonitoring(task.getTaskId());
        
        // Record completion metrics
        if (task.getStatus() == TaskStatus.COMPLETED) {
            taskMetrics.recordTaskCompleted(task.getTaskType(), task.getAgentKind(), 
                                          calculateTaskDuration(task), true);
        } else if (task.getStatus() == TaskStatus.FAILED) {
            taskMetrics.recordTaskCompleted(task.getTaskType(), task.getAgentKind(), 
                                          calculateTaskDuration(task), false);
            
            if (task.getError() != null && task.getError().getCode() != null) {
                taskMetrics.recordTaskError(task.getTaskType(), task.getAgentKind(), task.getError().getCode());
            }
        }
        
        // Handle post-completion actions
        if (task.getStatus() == TaskStatus.FAILED && !task.canRetry()) {
            handleTaskFailure(task);
        }
    }
    
    /**
     * Handle task failure and move to dead letter queue if needed.
     */
    private void handleTaskFailure(AgentTask task) {
        logger.warn("Handling final task failure: {}", task.getTaskId());
        
        try {
            // Move to dead letter queue
            firestore.collection(DEAD_LETTER_COLLECTION)
                    .document(task.getTaskId())
                    .set(task)
                    .get();
            
            // Notify administrators (in a real system, this would send alerts)
            logger.error("Task moved to dead letter queue - requires manual intervention: {} - {}", 
                        task.getTaskId(), task.getError() != null ? task.getError().getMessage() : "Unknown error");
            
        } catch (Exception e) {
            logger.error("Failed to move task to dead letter queue: {}", task.getTaskId(), e);
        }
    }
    
    /**
     * Scheduled task to monitor running tasks and handle timeouts.
     */
    @Scheduled(fixedDelay = TASK_MONITOR_INTERVAL_MS)
    public void monitorRunningTasks() {
        logger.debug("Running scheduled task monitoring");
        
        try {
            // Check for timed out tasks
            checkForTimedOutTasks();
            
            // Check for stale tasks
            checkForStaleTasks();
            
            // Check for zombie tasks
            checkForZombieTasks();
            
            // Clean up completed monitors
            cleanupCompletedMonitors();
            
        } catch (Exception e) {
            logger.error("Error during task monitoring", e);
        }
    }
    
    /**
     * Check for tasks that have exceeded their timeout.
     */
    private void checkForTimedOutTasks() {
        try {
            long currentTime = System.currentTimeMillis();
            
            QuerySnapshot runningTasks = firestore.collection(TASKS_COLLECTION)
                    .whereEqualTo("status", TaskStatus.RUNNING.name())
                    .get()
                    .get();
            
            for (DocumentSnapshot doc : runningTasks.getDocuments()) {
                AgentTask task = doc.toObject(AgentTask.class);
                if (task != null && task.hasTimedOut()) {
                    logger.warn("Task timed out: {} (started: {}, timeout: {}ms)", 
                               task.getTaskId(), task.getStartedAt(), task.getTimeoutMs());
                    
                    // Mark as failed due to timeout
                    task.markAsFailed("Task timed out", "TIMEOUT", null);
                    
                    // Save updated task
                    firestore.collection(TASKS_COLLECTION)
                            .document(task.getTaskId())
                            .set(task);
                    
                    // Handle completion
                    handleTaskCompletion(task);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to check for timed out tasks", e);
        }
    }
    
    /**
     * Check for stale tasks (running too long without updates).
     */
    private void checkForStaleTasks() {
        try {
            long staleThreshold = System.currentTimeMillis() - STALE_TASK_THRESHOLD_MS;
            
            QuerySnapshot staleTasks = firestore.collection(TASKS_COLLECTION)
                    .whereEqualTo("status", TaskStatus.RUNNING.name())
                    .whereLessThan("updatedAt", staleThreshold)
                    .get()
                    .get();
            
            for (DocumentSnapshot doc : staleTasks.getDocuments()) {
                AgentTask task = doc.toObject(AgentTask.class);
                if (task != null) {
                    logger.warn("Detected stale task: {} (last updated: {})", 
                               task.getTaskId(), new Date(task.getUpdatedAt()));
                    
                    // For now, just log. In production, might want to restart or investigate
                    // Could implement health checks or heartbeat mechanisms
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to check for stale tasks", e);
        }
    }
    
    /**
     * Check for zombie tasks (stuck in running state for too long).
     */
    private void checkForZombieTasks() {
        try {
            long zombieThreshold = System.currentTimeMillis() - ZOMBIE_TASK_THRESHOLD_MS;
            
            QuerySnapshot zombieTasks = firestore.collection(TASKS_COLLECTION)
                    .whereEqualTo("status", TaskStatus.RUNNING.name())
                    .whereLessThan("startedAt", zombieThreshold)
                    .get()
                    .get();
            
            for (DocumentSnapshot doc : zombieTasks.getDocuments()) {
                AgentTask task = doc.toObject(AgentTask.class);
                if (task != null) {
                    logger.error("Detected zombie task: {} (started: {})", 
                                task.getTaskId(), new Date(task.getStartedAt()));
                    
                    // Reset zombie task to pending for retry
                    task.setStatus(TaskStatus.PENDING);
                    task.setStartedAt(null);
                    task.setScheduledAt(System.currentTimeMillis());
                    
                    // Save updated task
                    firestore.collection(TASKS_COLLECTION)
                            .document(task.getTaskId())
                            .set(task);
                    
                    logger.info("Reset zombie task to pending: {}", task.getTaskId());
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to check for zombie tasks", e);
        }
    }
    
    /**
     * Clean up monitors for completed tasks.
     */
    private void cleanupCompletedMonitors() {
        activeTaskMonitors.entrySet().removeIf(entry -> {
            TaskMonitor monitor = entry.getValue();
            if (monitor.isExpired()) {
                logger.debug("Cleaning up expired monitor for task: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Monitor existing running tasks on startup.
     */
    private void monitorExistingRunningTasks() {
        try {
            QuerySnapshot runningTasks = firestore.collection(TASKS_COLLECTION)
                    .whereEqualTo("status", TaskStatus.RUNNING.name())
                    .get()
                    .get();
            
            for (DocumentSnapshot doc : runningTasks.getDocuments()) {
                AgentTask task = doc.toObject(AgentTask.class);
                if (task != null) {
                    startTaskMonitoring(task);
                }
            }
            
            logger.info("Started monitoring {} existing running tasks", runningTasks.size());
            
        } catch (Exception e) {
            logger.error("Failed to monitor existing running tasks", e);
        }
    }
    
    /**
     * Check if idempotency key is duplicate.
     */
    private boolean isDuplicateIdempotencyKey(String idempotencyKey) throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(TASKS_COLLECTION)
                .whereEqualTo("idempotencyKey", idempotencyKey)
                .limit(1)
                .get()
                .get();
        
        return !querySnapshot.isEmpty();
    }
    
    /**
     * Validate retry configuration.
     */
    private void validateRetryConfig(AgentTask.RetryConfig retryConfig, List<String> warnings) {
        if (retryConfig.getMaxAttempts() != null && retryConfig.getMaxAttempts() > 10) {
            warnings.add("Max attempts too high, using maximum of 10");
            retryConfig.setMaxAttempts(10);
        }
        
        if (retryConfig.getBaseDelayMs() != null && retryConfig.getBaseDelayMs() < 100) {
            warnings.add("Base delay too short, using minimum of 100ms");
            retryConfig.setBaseDelayMs(100L);
        }
        
        if (retryConfig.getMaxDelayMs() != null && retryConfig.getMaxDelayMs() > 3600000) {
            warnings.add("Max delay too long, using maximum of 1 hour");
            retryConfig.setMaxDelayMs(3600000L);
        }
    }
    
    /**
     * Calculate task duration.
     */
    private Long calculateTaskDuration(AgentTask task) {
        if (task.getStartedAt() != null && task.getCompletedAt() != null) {
            return task.getCompletedAt() - task.getStartedAt();
        }
        return null;
    }
    
    /**
     * Task validation result.
     */
    public static class TaskValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        
        public TaskValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        
        @Override
        public String toString() {
            return "TaskValidationResult{" +
                    "valid=" + valid +
                    ", errors=" + errors.size() +
                    ", warnings=" + warnings.size() +
                    '}';
        }
    }
    
    /**
     * Task monitor for tracking running tasks.
     */
    private static class TaskMonitor {
        private final String taskId;
        private final long timeoutMs;
        private final long startTime;
        
        public TaskMonitor(String taskId, Long timeoutMs) {
            this.taskId = taskId;
            this.timeoutMs = timeoutMs != null ? timeoutMs : 300000L; // Default 5 minutes
            this.startTime = System.currentTimeMillis();
        }
        
        public boolean isExpired() {
            return (System.currentTimeMillis() - startTime) > (timeoutMs + 60000); // Grace period
        }
        
        public String getTaskId() { return taskId; }
        public long getTimeoutMs() { return timeoutMs; }
        public long getStartTime() { return startTime; }
    }
}