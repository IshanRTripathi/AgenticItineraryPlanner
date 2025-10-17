package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for Google Places Text Search API response.
 */
public class PlaceSearchResponse {
    
    private List<PlaceSearchResult> results;
    
    private String status;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("next_page_token")
    private String nextPageToken;
    
    // Getters and Setters
    public List<PlaceSearchResult> getResults() {
        return results;
    }
    
    public void setResults(List<PlaceSearchResult> results) {
        this.results = results;
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
    
    public String getNextPageToken() {
        return nextPageToken;
    }
    
    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }
}
