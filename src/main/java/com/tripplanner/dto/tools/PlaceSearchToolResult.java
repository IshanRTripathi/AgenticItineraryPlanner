package com.tripplanner.dto.tools;

import com.tripplanner.dto.PlaceSuggestion;
import java.util.List;

/**
 * Response DTO for place search tool.
 * Used by /api/v1/tools/search-places endpoint.
 */
public class PlaceSearchToolResult {
    private boolean success;
    private List<PlaceSuggestion> suggestions;
    private String error;
    
    // Constructors
    public PlaceSearchToolResult() {}
    
    // Factory methods
    public static PlaceSearchToolResult success(List<PlaceSuggestion> suggestions) {
        PlaceSearchToolResult result = new PlaceSearchToolResult();
        result.setSuccess(true);
        result.setSuggestions(suggestions);
        return result;
    }
    
    public static PlaceSearchToolResult error(String error) {
        PlaceSearchToolResult result = new PlaceSearchToolResult();
        result.setSuccess(false);
        result.setError(error);
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public List<PlaceSuggestion> getSuggestions() {
        return suggestions;
    }
    
    public void setSuggestions(List<PlaceSuggestion> suggestions) {
        this.suggestions = suggestions;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    @Override
    public String toString() {
        return "PlaceSearchToolResult{" +
                "success=" + success +
                ", suggestions=" + (suggestions != null ? suggestions.size() + " items" : "null") +
                ", error='" + error + '\'' +
                '}';
    }
}
