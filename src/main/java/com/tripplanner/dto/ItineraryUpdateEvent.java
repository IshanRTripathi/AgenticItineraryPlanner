package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Event DTO for real-time itinerary generation updates sent via SSE.
 * This is different from PatchEvent as it's specifically for generation progress.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItineraryUpdateEvent {
    
    @JsonProperty("eventType")
    private String eventType;
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("executionId")
    private String executionId;
    
    @JsonProperty("dayNumber")
    private Integer dayNumber;
    
    @JsonProperty("nodeId")
    private String nodeId;
    
    @JsonProperty("data")
    private Object data;
    
    @JsonProperty("progress")
    private Integer progress;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("agentType")
    private String agentType;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    public ItineraryUpdateEvent() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ItineraryUpdateEvent(String eventType, String itineraryId, String executionId) {
        this();
        this.eventType = eventType;
        this.itineraryId = itineraryId;
        this.executionId = executionId;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ItineraryUpdateEvent event = new ItineraryUpdateEvent();
        
        public Builder eventType(String eventType) {
            event.eventType = eventType;
            return this;
        }
        
        public Builder itineraryId(String itineraryId) {
            event.itineraryId = itineraryId;
            return this;
        }
        
        public Builder executionId(String executionId) {
            event.executionId = executionId;
            return this;
        }
        
        public Builder dayNumber(Integer dayNumber) {
            event.dayNumber = dayNumber;
            return this;
        }
        
        public Builder nodeId(String nodeId) {
            event.nodeId = nodeId;
            return this;
        }
        
        public Builder data(Object data) {
            event.data = data;
            return this;
        }
        
        public Builder progress(Integer progress) {
            event.progress = progress;
            return this;
        }
        
        public Builder message(String message) {
            event.message = message;
            return this;
        }
        
        public Builder agentType(String agentType) {
            event.agentType = agentType;
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            event.timestamp = timestamp;
            return this;
        }
        
        public ItineraryUpdateEvent build() {
            return event;
        }
    }
    
    /**
     * Create a day completed event.
     */
    public static ItineraryUpdateEvent dayCompleted(String itineraryId, String executionId, 
                                                   Integer dayNumber, NormalizedDay dayData, 
                                                   Integer progress, String message) {
        return builder()
                .eventType("day_completed")
                .itineraryId(itineraryId)
                .executionId(executionId)
                .dayNumber(dayNumber)
                .data(dayData)
                .progress(progress)
                .message(message)
                .agentType("PLANNER")
                .build();
    }
    
    /**
     * Create a node enhanced event.
     */
    public static ItineraryUpdateEvent nodeEnhanced(String itineraryId, String executionId,
                                                   String nodeId, NormalizedNode nodeData,
                                                   String message, String agentType) {
        return builder()
                .eventType("node_enhanced")
                .itineraryId(itineraryId)
                .executionId(executionId)
                .nodeId(nodeId)
                .data(nodeData)
                .message(message)
                .agentType(agentType)
                .build();
    }
    
    /**
     * Create a progress update event.
     */
    public static ItineraryUpdateEvent progressUpdate(String itineraryId, String executionId,
                                                     Integer progress, String message, String agentType) {
        return builder()
                .eventType("progress_update")
                .itineraryId(itineraryId)
                .executionId(executionId)
                .progress(progress)
                .message(message)
                .agentType(agentType)
                .build();
    }
    
    /**
     * Create a generation complete event.
     */
    public static ItineraryUpdateEvent generationComplete(String itineraryId, String executionId,
                                                         NormalizedItinerary finalItinerary) {
        return builder()
                .eventType("generation_complete")
                .itineraryId(itineraryId)
                .executionId(executionId)
                .data(finalItinerary)
                .progress(100)
                .message("Itinerary generation completed")
                .agentType("orchestrator")
                .build();
    }
    
    // Getters and Setters
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public String getExecutionId() {
        return executionId;
    }
    
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
    
    public Integer getDayNumber() {
        return dayNumber;
    }
    
    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public Integer getProgress() {
        return progress;
    }
    
    public void setProgress(Integer progress) {
        this.progress = progress;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getAgentType() {
        return agentType;
    }
    
    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "ItineraryUpdateEvent{" +
                "eventType='" + eventType + '\'' +
                ", itineraryId='" + itineraryId + '\'' +
                ", executionId='" + executionId + '\'' +
                ", dayNumber=" + dayNumber +
                ", nodeId='" + nodeId + '\'' +
                ", progress=" + progress +
                ", message='" + message + '\'' +
                ", agentType='" + agentType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}