package com.tripplanner.service;

import com.google.cloud.firestore.*;
import com.tripplanner.dto.AgentTask;
import com.tripplanner.dto.AgentTask.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Durable agent task system that provides reliable task processing with persistence,
 * retry mechanisms, and automatic recovery across system restarts.
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "agent.task.system.enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
public class AgentTaskSystem {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentTaskSystem.class);
    
    @Autowired
    private Firestore firestore;
    
    @Autowired
    private IdempotencyManager idempotencyManager;
    
    private final TaskProcessor taskProcessor;
    private final TaskMetrics taskMetrics;
    private final TaskLifecycleManager lifecycleManager;
    
    // Task processing
    private final ExecutorService taskExecutor;
    private final Map<String, CompletableFuture<AgentTask>> runningTasks;
    private final Set<String> processedIdempotencyKeys;
    
    // Firestore collections
    private static final String TASKS_COLLECTION = "agent_tasks";
    private static final String DEAD_LETTER_COLLECTION = "dead_letter_tasks";
    
    // Configuration
    private static final int MAX_CONCURRENT_TASKS = 10;
    private static final int TASK_POLL_INTERVAL_MS = 5000; // 5 seconds
    private static final int CLEANUP_INTERVAL_MS = 300000; // 5 minutes
    private static final int COMPLETED_TASK_RETENTION_MS = 86400000; // 24 hours
    
    // Firestore listeners
    private ListenerRegistration taskListener;
    private volatile boolean isShuttingDown = false;
    
    public AgentTaskSystem(TaskProcessor taskProcessor, TaskMetrics taskMetrics, 
                          TaskLifecycleManager lifecycleManager) {
        this.taskProcessor = taskProcessor;
        this.taskMetrics = taskMetrics;
        this.lifecycleManager = lifecycleManager;
        this.taskExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_TASKS);
        this.runningTasks = new ConcurrentHashMap<>();
        this.processedIdempotencyKeys = ConcurrentHashMap.newKeySet();
    }
    
    @PostConstruct
    public void initialize() {
        logger.info("Initializing Agent Task System");
        
        // Set up Firestore listener for real-time task processing
        setupTaskListener();
        
        // Process any existing pending tasks on startup
        processPendingTasksOnStartup();
        
        logger.info("Agent Task System initialized successfully");
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Agent Task System");
        isShuttingDown = true;
        
        // Stop Firestore listener
        if (taskListener != null) {
            taskListener.remove();
        }
        
        // Wait for running tasks to complete (with timeout)
        try {
            taskExecutor.shutdown();
            if (!taskExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Task executor did not terminate within 30 seconds, forcing shutdown");
                taskExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for task executor shutdown", e);
            taskExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Agent Task System shutdown complete");
    }
    
    /**
     * Submit a new task for processing.
     * Returns the task ID if successful, or existing task ID if duplicate.
     */
    public String submitTask(AgentTask task) {
        logger.info("Submitting task: {} (type: {}, agent: {})", 
                   task.getTaskId(), task.getTaskType(), task.getAgentKind());
        
        try {
            // Validate task using lifecycle manager
            TaskLifecycleManager.TaskValidationResult validation = lifecycleManager.validateTaskSubmission(task);
            if (!validation.isValid()) {
                throw new IllegalArgumentException("Invalid task: " + validation.getErrors());
            }
            
            // Log warnings if any
            if (!validation.getWarnings().isEmpty()) {
                logger.warn("Task validation warnings for {}: {}", task.getTaskId(), validation.getWarnings());
            }
            
            // Check for duplicate using idempotency key
            if (task.getIdempotencyKey() != null) {
                if (!idempotencyManager.isValidIdempotencyKey(task.getIdempotencyKey())) {
                    throw new IllegalArgumentException("Invalid idempotency key format: " + task.getIdempotencyKey());
                }
                
                Optional<IdempotencyManager.IdempotencyRecord> existingRecord = 
                    idempotencyManager.getExistingOperation(task.getIdempotencyKey());
                
                if (existingRecord.isPresent()) {
                    String existingTaskId = (String) existingRecord.get().getResult();
                    logger.info("Duplicate task detected via idempotency key, returning existing task ID: {}", existingTaskId);
                    return existingTaskId;
                }
            }
            
            // Save task to Firestore
            saveTask(task);
            
            // Store in idempotency manager if key provided
            if (task.getIdempotencyKey() != null) {
                idempotencyManager.storeOperationResult(
                    task.getIdempotencyKey(), 
                    task.getTaskId(), 
                    "task_creation"
                );
            }
            
            // Record metrics
            taskMetrics.recordTaskSubmitted(task.getTaskType(), task.getAgentKind());
            
            logger.info("Task submitted successfully: {}", task.getTaskId());
            return task.getTaskId();
            
        } catch (Exception e) {
            logger.error("Failed to submit task: {}", task.getTaskId(), e);
            throw new RuntimeException("Failed to submit task", e);
        }
    }
    
    /**
     * Get task status and details.
     */
    public Optional<AgentTask> getTask(String taskId) {
        try {
            DocumentSnapshot doc = firestore.collection(TASKS_COLLECTION)
                    .document(taskId)
                    .get()
                    .get();
            
            if (doc.exists()) {
                return Optional.ofNullable(doc.toObject(AgentTask.class));
            } else {
                return Optional.empty();
            }
            
        } catch (Exception e) {
            logger.error("Failed to get task: {}", taskId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Cancel a task if it's not already running.
     */
    public boolean cancelTask(String taskId, String reason) {
        logger.info("Cancelling task: {} - {}", taskId, reason);
        
        try {
            Optional<AgentTask> taskOpt = getTask(taskId);
            if (taskOpt.isEmpty()) {
                logger.warn("Task not found for cancellation: {}", taskId);
                return false;
            }
            
            AgentTask task = taskOpt.get();
            
            // Can only cancel pending tasks
            if (task.getStatus() != TaskStatus.PENDING) {
                logger.warn("Cannot cancel task in status: {} - {}", task.getStatus(), taskId);
                return false;
            }
            
            // Mark as cancelled
            task.markAsCancelled(reason);
            saveTask(task);
            
            logger.info("Task cancelled successfully: {}", taskId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to cancel task: {}", taskId, e);
            return false;
        }
    }
    
    /**
     * Get tasks for a specific itinerary.
     */
    public List<AgentTask> getTasksForItinerary(String itineraryId) {
        try {
            QuerySnapshot querySnapshot = firestore.collection(TASKS_COLLECTION)
                    .whereEqualTo("itineraryId", itineraryId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .get();
            
            return querySnapshot.getDocuments().stream()
                    .map(doc -> doc.toObject(AgentTask.class))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Failed to get tasks for itinerary: {}", itineraryId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get system statistics.
     */
    public TaskSystemStats getStats() {
        TaskMetrics.SystemMetrics systemMetrics = taskMetrics.getSystemMetrics();
        
        return new TaskSystemStats(
            systemMetrics,
            runningTasks.size(),
            processedIdempotencyKeys.size(),
            getTaskCountByStatus()
        );
    }
    
    /**
     * Set up Firestore listener for real-time task processing.
     */
    private void setupTaskListener() {
        logger.info("Setting up Firestore task listener");
        
        Query query = firestore.collection(TASKS_COLLECTION)
                .whereEqualTo("status", TaskStatus.PENDING.name())
                .orderBy("priority", Query.Direction.DESCENDING)
                .orderBy("scheduledAt", Query.Direction.ASCENDING);
        
        taskListener = query.addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                logger.error("Error in task listener", error);
                return;
            }
            
            if (querySnapshot != null && !isShuttingDown) {
                for (DocumentChange change : querySnapshot.getDocumentChanges()) {
                    if (change.getType() == DocumentChange.Type.ADDED) {
                        AgentTask task = change.getDocument().toObject(AgentTask.class);
                        if (task != null && taskProcessor.shouldProcessTask(task)) {
                            processTaskAsync(task);
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Process pending tasks on system startup.
     */
    private void processPendingTasksOnStartup() {
        logger.info("Processing pending tasks on startup");
        
        try {
            // Find tasks that were running when system shut down
            QuerySnapshot runningTasks = firestore.collection(TASKS_COLLECTION)
                    .whereEqualTo("status", TaskStatus.RUNNING.name())
                    .get()
                    .get();
            
            for (DocumentSnapshot doc : runningTasks.getDocuments()) {
                AgentTask task = doc.toObject(AgentTask.class);
                if (task != null) {
                    logger.info("Found orphaned running task, marking for retry: {}", task.getTaskId());
                    task.setStatus(TaskStatus.PENDING);
                    task.setStartedAt(null);
                    saveTask(task);
                }
            }
            
            // Process pending tasks
            QuerySnapshot pendingTasks = null;
            try {
                // Try compound query (requires Firestore index)
                pendingTasks = firestore.collection(TASKS_COLLECTION)
                        .whereEqualTo("status", TaskStatus.PENDING.name())
                        .whereLessThanOrEqualTo("scheduledAt", System.currentTimeMillis())
                        .orderBy("scheduledAt", Query.Direction.ASCENDING)
                        .limit(MAX_CONCURRENT_TASKS)
                        .get()
                        .get();
            } catch (Exception indexError) {
                // Fallback to simpler query if index doesn't exist
                logger.warn("Firestore compound index not available, using fallback query. " +
                           "Create index at: https://console.firebase.google.com/project/tripaiplanner-4c951/firestore/indexes");
                logger.warn("Index error: {}", indexError.getMessage());
                
                try {
                    // Simpler query without compound index
                    pendingTasks = firestore.collection(TASKS_COLLECTION)
                            .whereEqualTo("status", TaskStatus.PENDING.name())
                            .limit(MAX_CONCURRENT_TASKS * 2) // Get more to filter in memory
                            .get()
                            .get();
                } catch (Exception fallbackError) {
                    logger.error("Fallback query also failed", fallbackError);
                    throw fallbackError;
                }
            }
            
            if (pendingTasks != null) {
                // Filter and sort in memory if using fallback query
                long currentTime = System.currentTimeMillis();
                List<AgentTask> tasksToProcess = pendingTasks.getDocuments().stream()
                        .map(doc -> doc.toObject(AgentTask.class))
                        .filter(task -> task != null && 
                                       task.getScheduledAt() <= currentTime &&
                                       taskProcessor.shouldProcessTask(task))
                        .sorted((t1, t2) -> Long.compare(t1.getScheduledAt(), t2.getScheduledAt()))
                        .limit(MAX_CONCURRENT_TASKS)
                        .collect(java.util.stream.Collectors.toList());
                
                for (AgentTask task : tasksToProcess) {
                    processTaskAsync(task);
                }
                
                logger.info("Startup task processing complete. Processed {} pending tasks", tasksToProcess.size());
            }
            
        } catch (Exception e) {
            logger.error("Failed to process pending tasks on startup", e);
        }
    }
    
    /**
     * Process a task asynchronously.
     */
    @Async
    public void processTaskAsync(AgentTask task) {
        if (isShuttingDown || runningTasks.containsKey(task.getTaskId())) {
            return; // Skip if shutting down or already processing
        }
        
        logger.debug("Starting async processing for task: {}", task.getTaskId());
        
        // Start monitoring the task
        lifecycleManager.startTaskMonitoring(task);
        
        CompletableFuture<AgentTask> future = taskProcessor.processTask(task)
                .whenComplete((result, throwable) -> {
                    runningTasks.remove(task.getTaskId());
                    
                    if (throwable != null) {
                        logger.error("Task processing failed: {}", task.getTaskId(), throwable);
                        result = task; // Use the task as-is if processing failed
                        result.markAsFailed("Processing failed: " + throwable.getMessage(), 
                                          "PROCESSING_ERROR", throwable);
                    }
                    
                    // Save updated task
                    try {
                        saveTask(result);
                        
                        // Handle completion through lifecycle manager
                        lifecycleManager.handleTaskCompletion(result);
                        
                        // Handle retry if needed
                        if (result.getStatus() == TaskStatus.FAILED && result.canRetry()) {
                            logger.info("Scheduling retry for task: {} (attempt {})", 
                                       result.getTaskId(), result.getCurrentAttemptNumber());
                            result.scheduleRetry();
                            saveTask(result);
                            taskMetrics.recordTaskRetry(result.getTaskType(), result.getAgentKind(), 
                                                      result.getCurrentAttemptNumber());
                        }
                        
                    } catch (Exception e) {
                        logger.error("Failed to save task result: {}", task.getTaskId(), e);
                    }
                });
        
        runningTasks.put(task.getTaskId(), future);
    }
    
    /**
     * Save task to Firestore.
     */
    private void saveTask(AgentTask task) {
        try {
            firestore.collection(TASKS_COLLECTION)
                    .document(task.getTaskId())
                    .set(task)
                    .get();
                    
        } catch (Exception e) {
            logger.error("Failed to save task: {}", task.getTaskId(), e);
            throw new RuntimeException("Failed to save task", e);
        }
    }
    

    
    /**
     * Move failed task to dead letter queue.
     */
    private void moveToDeadLetterQueue(AgentTask task) {
        try {
            logger.warn("Moving task to dead letter queue: {}", task.getTaskId());
            
            // Save to dead letter collection
            firestore.collection(DEAD_LETTER_COLLECTION)
                    .document(task.getTaskId())
                    .set(task)
                    .get();
            
            // Remove from main tasks collection
            firestore.collection(TASKS_COLLECTION)
                    .document(task.getTaskId())
                    .delete()
                    .get();
            
            logger.info("Task moved to dead letter queue: {}", task.getTaskId());
            
        } catch (Exception e) {
            logger.error("Failed to move task to dead letter queue: {}", task.getTaskId(), e);
        }
    }
    
    /**
     * Get task count by status.
     */
    private Map<TaskStatus, Long> getTaskCountByStatus() {
        Map<TaskStatus, Long> counts = new HashMap<>();
        
        try {
            for (TaskStatus status : TaskStatus.values()) {
                QuerySnapshot querySnapshot = firestore.collection(TASKS_COLLECTION)
                        .whereEqualTo("status", status.name())
                        .get()
                        .get();
                
                counts.put(status, (long) querySnapshot.size());
            }
            
        } catch (Exception e) {
            logger.error("Failed to get task counts by status", e);
        }
        
        return counts;
    }
    
    /**
     * Scheduled cleanup of old completed tasks.
     */
    @Scheduled(fixedDelay = CLEANUP_INTERVAL_MS)
    public void cleanupOldTasks() {
        if (isShuttingDown) {
            return;
        }
        
        logger.debug("Running scheduled task cleanup");
        
        try {
            long cutoffTime = System.currentTimeMillis() - COMPLETED_TASK_RETENTION_MS;
            
            QuerySnapshot oldTasks = firestore.collection(TASKS_COLLECTION)
                    .whereIn("status", Arrays.asList(TaskStatus.COMPLETED.name(), TaskStatus.CANCELLED.name()))
                    .whereLessThan("completedAt", cutoffTime)
                    .get()
                    .get();
            
            int deletedCount = 0;
            for (DocumentSnapshot doc : oldTasks.getDocuments()) {
                doc.getReference().delete();
                deletedCount++;
            }
            
            if (deletedCount > 0) {
                logger.info("Cleaned up {} old completed tasks", deletedCount);
            }
            
        } catch (Exception e) {
            logger.error("Failed to cleanup old tasks", e);
        }
    }
    
    /**
     * Task system statistics.
     */
    public static class TaskSystemStats {
        private final TaskMetrics.SystemMetrics systemMetrics;
        private final int currentlyRunningTasks;
        private final int processedIdempotencyKeys;
        private final Map<TaskStatus, Long> taskCountsByStatus;
        
        public TaskSystemStats(TaskMetrics.SystemMetrics systemMetrics, int currentlyRunningTasks,
                             int processedIdempotencyKeys, Map<TaskStatus, Long> taskCountsByStatus) {
            this.systemMetrics = systemMetrics;
            this.currentlyRunningTasks = currentlyRunningTasks;
            this.processedIdempotencyKeys = processedIdempotencyKeys;
            this.taskCountsByStatus = taskCountsByStatus;
        }
        
        public TaskMetrics.SystemMetrics getSystemMetrics() { return systemMetrics; }
        public int getCurrentlyRunningTasks() { return currentlyRunningTasks; }
        public int getProcessedIdempotencyKeys() { return processedIdempotencyKeys; }
        public Map<TaskStatus, Long> getTaskCountsByStatus() { return taskCountsByStatus; }
        
        @Override
        public String toString() {
            return "TaskSystemStats{" +
                    "systemMetrics=" + systemMetrics +
                    ", currentlyRunningTasks=" + currentlyRunningTasks +
                    ", processedIdempotencyKeys=" + processedIdempotencyKeys +
                    ", taskCountsByStatus=" + taskCountsByStatus +
                    '}';
        }
    }
}