package com.tripplanner.dto.tools;

/**
 * Request DTO for place search tool.
 * Used by /api/v1/tools/search-places endpoint.
 */
public class PlaceSearchToolRequest {
    private String itineraryId;
    private String query;
    private String location;
    private String type;
    private Integer maxResults;
    
    // Constructors
    public PlaceSearchToolRequest() {}
    
    public PlaceSearchToolRequest(String itineraryId, String query, String location) {
        this.itineraryId = itineraryId;
        this.query = query;
        this.location = location;
    }
    
    // Getters and Setters
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Integer getMaxResults() {
        return maxResults;
    }
    
    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }
    
    @Override
    public String toString() {
        return "PlaceSearchToolRequest{" +
                "itineraryId='" + itineraryId + '\'' +
                ", query='" + query + '\'' +
                ", location='" + location + '\'' +
                ", type='" + type + '\'' +
                ", maxResults=" + maxResults +
                '}';
    }
}
