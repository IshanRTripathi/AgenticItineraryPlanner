package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Enhanced response DTO for itinerary creation that provides immediate feedback
 * and real-time update information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItineraryCreationResponse {
    
    @JsonProperty("itinerary")
    private ItineraryDto itinerary;
    
    @JsonProperty("executionId")
    private String executionId;
    
    @JsonProperty("sseEndpoint")
    private String sseEndpoint;
    
    @JsonProperty("estimatedCompletion")
    private LocalDateTime estimatedCompletion;
    
    @JsonProperty("status")
    private CreationStatus status;
    
    @JsonProperty("stages")
    private List<AgentExecutionStage> stages;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    public ItineraryCreationResponse() {}
    
    public ItineraryCreationResponse(ItineraryDto itinerary, String executionId, String sseEndpoint,
                                   LocalDateTime estimatedCompletion, CreationStatus status) {
        this.itinerary = itinerary;
        this.executionId = executionId;
        this.sseEndpoint = sseEndpoint;
        this.estimatedCompletion = estimatedCompletion;
        this.status = status;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ItineraryCreationResponse response = new ItineraryCreationResponse();
        
        public Builder itinerary(ItineraryDto itinerary) {
            response.itinerary = itinerary;
            return this;
        }
        
        public Builder executionId(String executionId) {
            response.executionId = executionId;
            return this;
        }
        
        public Builder sseEndpoint(String sseEndpoint) {
            response.sseEndpoint = sseEndpoint;
            return this;
        }
        
        public Builder estimatedCompletion(LocalDateTime estimatedCompletion) {
            response.estimatedCompletion = estimatedCompletion;
            return this;
        }
        
        public Builder status(CreationStatus status) {
            response.status = status;
            return this;
        }
        
        public Builder stages(List<AgentExecutionStage> stages) {
            response.stages = stages;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            response.errorMessage = errorMessage;
            return this;
        }
        
        public ItineraryCreationResponse build() {
            return response;
        }
    }
    
    /**
     * Create an error response.
     */
    public static ItineraryCreationResponse error(String message) {
        ItineraryCreationResponse response = new ItineraryCreationResponse();
        response.status = CreationStatus.FAILED;
        response.errorMessage = message;
        return response;
    }
    
    // Getters and Setters
    public ItineraryDto getItinerary() {
        return itinerary;
    }
    
    public void setItinerary(ItineraryDto itinerary) {
        this.itinerary = itinerary;
    }
    
    public String getExecutionId() {
        return executionId;
    }
    
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
    
    public String getSseEndpoint() {
        return sseEndpoint;
    }
    
    public void setSseEndpoint(String sseEndpoint) {
        this.sseEndpoint = sseEndpoint;
    }
    
    public LocalDateTime getEstimatedCompletion() {
        return estimatedCompletion;
    }
    
    public void setEstimatedCompletion(LocalDateTime estimatedCompletion) {
        this.estimatedCompletion = estimatedCompletion;
    }
    
    public CreationStatus getStatus() {
        return status;
    }
    
    public void setStatus(CreationStatus status) {
        this.status = status;
    }
    
    public List<AgentExecutionStage> getStages() {
        return stages;
    }
    
    public void setStages(List<AgentExecutionStage> stages) {
        this.stages = stages;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    @Override
    public String toString() {
        return "ItineraryCreationResponse{" +
                "itinerary=" + (itinerary != null ? itinerary.getId() : "null") +
                ", executionId='" + executionId + '\'' +
                ", sseEndpoint='" + sseEndpoint + '\'' +
                ", estimatedCompletion=" + estimatedCompletion +
                ", status=" + status +
                ", stages=" + (stages != null ? stages.size() : 0) + " stages" +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}