package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response wrapper for Google Places API place details response.
 * Ignores unknown properties to be resilient to API changes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceDetailsResponse {
    
    @JsonProperty("result")
    private PlaceDetails result;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("info_messages")
    private List<String> infoMessages;
    
    @JsonProperty("html_attributions")
    private List<String> htmlAttributions;
    
    public PlaceDetailsResponse() {}
    
    // Getters and Setters
    public PlaceDetails getResult() {
        return result;
    }
    
    public void setResult(PlaceDetails result) {
        this.result = result;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public List<String> getInfoMessages() {
        return infoMessages;
    }
    
    public void setInfoMessages(List<String> infoMessages) {
        this.infoMessages = infoMessages;
    }
    
    public List<String> getHtmlAttributions() {
        return htmlAttributions;
    }
    
    public void setHtmlAttributions(List<String> htmlAttributions) {
        this.htmlAttributions = htmlAttributions;
    }
    
    /**
     * Check if the API response was successful.
     */
    public boolean isSuccessful() {
        return "OK".equals(status);
    }
    
    /**
     * Check if the response indicates rate limiting.
     */
    public boolean isRateLimited() {
        return "OVER_QUERY_LIMIT".equals(status);
    }
    
    /**
     * Check if the place was not found.
     */
    public boolean isNotFound() {
        return "NOT_FOUND".equals(status) || "ZERO_RESULTS".equals(status);
    }
    
    /**
     * Check if there was an invalid request.
     */
    public boolean isInvalidRequest() {
        return "INVALID_REQUEST".equals(status);
    }
    
    /**
     * Check if the request was denied (usually API key issues).
     */
    public boolean isRequestDenied() {
        return "REQUEST_DENIED".equals(status);
    }
    
    /**
     * Check if there was an unknown error.
     */
    public boolean isUnknownError() {
        return "UNKNOWN_ERROR".equals(status);
    }
    
    /**
     * Get a user-friendly error message.
     */
    public String getUserFriendlyErrorMessage() {
        if (isSuccessful()) {
            return null;
        }
        
        switch (status) {
            case "OVER_QUERY_LIMIT":
                return "Service temporarily unavailable due to high demand. Please try again later.";
            case "REQUEST_DENIED":
                return "Access denied. Please check your API configuration.";
            case "INVALID_REQUEST":
                return "Invalid request. Please check the place ID and try again.";
            case "NOT_FOUND":
            case "ZERO_RESULTS":
                return "Place not found. The place may no longer exist or the ID may be incorrect.";
            case "UNKNOWN_ERROR":
                return "An unexpected error occurred. Please try again.";
            default:
                return errorMessage != null ? errorMessage : "An error occurred while retrieving place details.";
        }
    }
    
    @Override
    public String toString() {
        return "PlaceDetailsResponse{" +
                "result=" + result +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", infoMessages=" + infoMessages +
                '}';
    }
}