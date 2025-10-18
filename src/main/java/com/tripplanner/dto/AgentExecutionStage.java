package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single stage in the agent execution pipeline.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentExecutionStage {
    
    @JsonProperty("stageName")
    private String stageName;
    
    @JsonProperty("agentType")
    private String agentType;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("estimatedDurationMs")
    private Long estimatedDurationMs;
    
    @JsonProperty("status")
    private StageStatus status;
    
    @JsonProperty("progress")
    private Integer progress; // 0-100
    
    @JsonProperty("startedAt")
    private Long startedAt; // timestamp
    
    @JsonProperty("completedAt")
    private Long completedAt; // timestamp
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    public AgentExecutionStage() {
        this.status = StageStatus.PENDING;
        this.progress = 0;
    }
    
    public AgentExecutionStage(String stageName, String agentType, String description) {
        this();
        this.stageName = stageName;
        this.agentType = agentType;
        this.description = description;
    }
    
    public AgentExecutionStage(String stageName, String agentType, String description, Long estimatedDurationMs) {
        this(stageName, agentType, description);
        this.estimatedDurationMs = estimatedDurationMs;
    }
    
    /**
     * Mark stage as started.
     */
    public void markStarted() {
        this.status = StageStatus.RUNNING;
        this.startedAt = System.currentTimeMillis();
        this.progress = 0;
    }
    
    /**
     * Update stage progress.
     */
    public void updateProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        if (this.status == StageStatus.PENDING) {
            markStarted();
        }
    }
    
    /**
     * Mark stage as completed.
     */
    public void markCompleted() {
        this.status = StageStatus.COMPLETED;
        this.completedAt = System.currentTimeMillis();
        this.progress = 100;
    }
    
    /**
     * Mark stage as failed.
     */
    public void markFailed(String errorMessage) {
        this.status = StageStatus.FAILED;
        this.completedAt = System.currentTimeMillis();
        this.errorMessage = errorMessage;
    }
    
    /**
     * Get actual duration if stage has started.
     */
    public Long getActualDurationMs() {
        if (startedAt == null) {
            return null;
        }
        
        long endTime = completedAt != null ? completedAt : System.currentTimeMillis();
        return endTime - startedAt;
    }
    
    /**
     * Check if stage is completed (successfully or with failure).
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isCompleted() {
        return status == StageStatus.COMPLETED || status == StageStatus.FAILED;
    }
    
    /**
     * Check if stage is running.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isRunning() {
        return status == StageStatus.RUNNING;
    }
    
    // Getters and Setters
    public String getStageName() {
        return stageName;
    }
    
    public void setStageName(String stageName) {
        this.stageName = stageName;
    }
    
    public String getAgentType() {
        return agentType;
    }
    
    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Long getEstimatedDurationMs() {
        return estimatedDurationMs;
    }
    
    public void setEstimatedDurationMs(Long estimatedDurationMs) {
        this.estimatedDurationMs = estimatedDurationMs;
    }
    
    public StageStatus getStatus() {
        return status;
    }
    
    public void setStatus(StageStatus status) {
        this.status = status;
    }
    
    public Integer getProgress() {
        return progress;
    }
    
    public void setProgress(Integer progress) {
        this.progress = progress;
    }
    
    public Long getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Long startedAt) {
        this.startedAt = startedAt;
    }
    
    public Long getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * Stage status enum.
     */
    public enum StageStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }
    
    @Override
    public String toString() {
        return "AgentExecutionStage{" +
                "stageName='" + stageName + '\'' +
                ", agentType='" + agentType + '\'' +
                ", status=" + status +
                ", progress=" + progress +
                ", estimatedDurationMs=" + estimatedDurationMs +
                '}';
    }
}