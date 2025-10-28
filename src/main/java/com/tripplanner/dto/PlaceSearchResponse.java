package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for Google Places Text Search API response.
 * Matches the exact structure from Google Places API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceSearchResponse {
    
    @JsonProperty("html_attributions")
    private List<String> htmlAttributions;
    
    @JsonProperty("results")
    private List<PlaceSearchResult> results;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("next_page_token")
    private String nextPageToken;
    
    // Getters and Setters
    public List<String> getHtmlAttributions() {
        return htmlAttributions;
    }
    
    public void setHtmlAttributions(List<String> htmlAttributions) {
        this.htmlAttributions = htmlAttributions;
    }
    
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
    
    @Override
    public String toString() {
        return "PlaceSearchResponse{" +
                "status='" + status + '\'' +
                ", results=" + (results != null ? results.size() + " items" : "null") +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
