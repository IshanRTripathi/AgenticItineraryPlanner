package com.tripplanner.service;

import com.tripplanner.dto.AgentTask;
import com.tripplanner.dto.AgentTask.TaskAttempt;
import com.tripplanner.dto.AgentTask.TaskStatus;
import com.tripplanner.agents.BaseAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service responsible for processing individual agent tasks.
 * Handles task execution, timeout management, and result capture.
 */
@Service
public class TaskProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskProcessor.class);
    
    private final AgentRegistry agentRegistry;
    private final TaskMetrics taskMetrics;
    private final RetryPolicy retryPolicy;
    
    public TaskProcessor(AgentRegistry agentRegistry, TaskMetrics taskMetrics, RetryPolicy retryPolicy) {
        this.agentRegistry = agentRegistry;
        this.taskMetrics = taskMetrics;
        this.retryPolicy = retryPolicy;
    }
    
    /**
     * Process a single agent task.
     * Returns a CompletableFuture that completes when the task is done.
     */
    public CompletableFuture<AgentTask> processTask(AgentTask task) {
        logger.info("Processing task: {}", task.getTaskId());
        
        return CompletableFuture.supplyAsync(() -> {
            TaskAttempt attempt = new TaskAttempt(task.getCurrentAttemptNumber());
            task.addAttempt(attempt);
            
            try {
                // Mark task as started
                task.markAsStarted();
                
                // Record metrics
                taskMetrics.recordTaskStarted(task.getTaskType(), task.getAgentKind());
                
                // Execute the task
                Map<String, Object> result = executeTask(task);
                
                // Mark task as completed
                task.markAsCompleted(result);
                attempt.markCompleted(TaskStatus.COMPLETED, null);
                
                // Record success metrics
                taskMetrics.recordTaskCompleted(task.getTaskType(), task.getAgentKind(), 
                                              attempt.getDurationMs(), true);
                
                logger.info("Task completed successfully: {}", task.getTaskId());
                return task;
                
            } catch (TimeoutException e) {
                return handleTaskFailure(task, attempt, e, "Task timed out after " + task.getTimeoutMs() + "ms", "TIMEOUT");
                
            } catch (Exception e) {
                return handleTaskFailure(task, attempt, e, "Task execution failed: " + e.getMessage(), "EXECUTION_ERROR");
            }
        });
    }
    
    /**
     * Execute the actual task logic.
     */
    private Map<String, Object> executeTask(AgentTask task) throws Exception {
        logger.debug("Executing task: {} with agent kind: {}", task.getTaskId(), task.getAgentKind());
        
        // Get the appropriate agent for this task
        BaseAgent agent = getAgentForTask(task);
        if (agent == null) {
            throw new IllegalStateException("No agent available for task type: " + task.getAgentKind());
        }
        
        // Create agent request from task payload
        BaseAgent.AgentRequest<?> agentRequest = createAgentRequest(task);
        
        // Execute with timeout
        CompletableFuture<Object> executionFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return agent.execute(task.getItineraryId(), agentRequest);
            } catch (Exception e) {
                throw new RuntimeException("Agent execution failed", e);
            }
        });
        
        // Wait for completion with timeout
        Object result = executionFuture.get(task.getTimeoutMs(), TimeUnit.MILLISECONDS);
        
        // Convert result to map
        Map<String, Object> resultMap = new HashMap<>();
        if (result != null) {
            resultMap.put("result", result);
            resultMap.put("resultType", result.getClass().getSimpleName());
        }
        
        return resultMap;
    }
    
    /**
     * Get the appropriate agent for a task.
     */
    private BaseAgent getAgentForTask(AgentTask task) {
        try {
            // Try to get agent by kind first
            BaseAgent agent = agentRegistry.getAgentByKind(task.getAgentKind());
            if (agent != null) {
                return agent;
            }
            
            // Fallback to getting agents by task type
            var agents = agentRegistry.getAgentsForTask(task.getTaskType());
            if (!agents.isEmpty()) {
                return agents.get(0); // Return first suitable agent
            }
            
            logger.warn("No agent found for task: {} (kind: {}, type: {})", 
                       task.getTaskId(), task.getAgentKind(), task.getTaskType());
            return null;
            
        } catch (Exception e) {
            logger.error("Failed to get agent for task: {}", task.getTaskId(), e);
            return null;
        }
    }
    
    /**
     * Create an agent request from task payload.
     */
    private BaseAgent.AgentRequest<?> createAgentRequest(AgentTask task) {
        Map<String, Object> payload = task.getPayload();
        
        // Determine response type based on task type
        Class<?> responseType = determineResponseType(task.getTaskType());
        
        // Create request with payload data
        return new BaseAgent.AgentRequest<>(payload, responseType);
    }
    
    /**
     * Determine the expected response type for a task type.
     */
    private Class<?> determineResponseType(String taskType) {
        switch (taskType.toLowerCase()) {
            case "enrich":
            case "ENRICHMENT":
                return com.tripplanner.service.ChangeEngine.ApplyResult.class;
            case "plan":
            case "planning":
                return com.tripplanner.dto.ItineraryDto.class;
            case "book":
            case "booking":
                return com.tripplanner.dto.BookingResult.class;
            case "places":
                return com.tripplanner.agents.PlacesAgent.PlacesResponse.class;
            default:
                return Object.class;
        }
    }
    
    /**
     * Check if a task should be processed (not already running or completed).
     */
    public boolean shouldProcessTask(AgentTask task) {
        if (task == null) {
            return false;
        }
        
        // Don't process terminal tasks
        if (task.isTerminal()) {
            return false;
        }
        
        // Don't process running tasks
        if (task.getStatus() == TaskStatus.RUNNING) {
            return false;
        }
        
        // Don't process tasks that are scheduled for the future
        if (task.getScheduledAt() != null && task.getScheduledAt() > System.currentTimeMillis()) {
            return false;
        }
        
        // Check if task has timed out (mark as failed if so)
        if (task.hasTimedOut()) {
            logger.warn("Task has timed out: {}", task.getTaskId());
            task.markAsFailed("Task timed out", "TIMEOUT", null);
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate task before processing.
     */
    public boolean validateTask(AgentTask task) {
        if (task == null) {
            logger.warn("Cannot process null task");
            return false;
        }
        
        if (task.getTaskId() == null || task.getTaskId().trim().isEmpty()) {
            logger.warn("Task has no ID: {}", task);
            return false;
        }
        
        if (task.getTaskType() == null || task.getTaskType().trim().isEmpty()) {
            logger.warn("Task has no type: {}", task.getTaskId());
            return false;
        }
        
        if (task.getAgentKind() == null || task.getAgentKind().trim().isEmpty()) {
            logger.warn("Task has no agent kind: {}", task.getTaskId());
            return false;
        }
        
        if (task.getItineraryId() == null || task.getItineraryId().trim().isEmpty()) {
            logger.warn("Task has no itinerary ID: {}", task.getTaskId());
            return false;
        }
        
        return true;
    }
    

    
    /**
     * Generate a unique task ID.
     */
    private static String generateTaskId(String taskType, String itineraryId) {
        return String.format("task_%s_%s_%d", 
                           taskType, 
                           itineraryId, 
                           System.currentTimeMillis());
    }
    
    /**
     * Handle task failure with retry policy integration.
     */
    private AgentTask handleTaskFailure(AgentTask task, TaskAttempt attempt, Exception exception, 
                                      String errorMessage, String errorCode) {
        logger.error("Task failed: {} - {}", task.getTaskId(), errorMessage, exception);
        
        // Record retry attempt
        retryPolicy.recordRetryAttempt(task.getTaskId(), task.getTaskType(), 
                                     task.getCurrentAttemptNumber(), exception);
        
        // Check if we should retry
        boolean shouldRetry = retryPolicy.shouldRetry(task.getTaskId(), task.getTaskType(), 
                                                    task.getCurrentAttemptNumber(), exception);
        
        if (shouldRetry) {
            // Mark attempt as failed but keep task in retry state
            AgentTask.TaskError taskError = new AgentTask.TaskError(errorMessage, errorCode, exception);
            attempt.markCompleted(AgentTask.TaskStatus.FAILED, taskError);
            task.incrementRetryCount();
            task.setStatus(TaskStatus.PENDING); // Reset to pending for retry
            
            // Calculate retry delay
            long retryDelay = retryPolicy.calculateRetryDelay(task.getTaskId(), task.getTaskType(), 
                                                            task.getCurrentAttemptNumber() - 1);
            task.setNextRetryTime(System.currentTimeMillis() + retryDelay);
            
            logger.info("Task {} will be retried in {}ms (attempt {}/{})", 
                       task.getTaskId(), retryDelay, task.getCurrentAttemptNumber(), 
                       retryPolicy.getRetryConfig(task.getTaskType()).getMaxAttempts());
            
            taskMetrics.recordTaskRetry(task.getTaskType(), task.getAgentKind(), task.getCurrentAttemptNumber());
            
        } else {
            // Mark task as permanently failed
            task.markAsFailed(errorMessage, errorCode, exception);
            attempt.markCompleted(TaskStatus.FAILED, task.getError());
            
            // Mark operation as failed in retry policy
            retryPolicy.markOperationFailed(task.getTaskId());
            
            taskMetrics.recordTaskCompleted(task.getTaskType(), task.getAgentKind(), 
                                          attempt.getDurationMs(), false);
        }
        
        return task;
    }
}