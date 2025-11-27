package com.tripplanner.dto.tools;

import com.tripplanner.dto.Coordinates;
import java.util.List;

/**
 * Request DTO for optimize route tool.
 * P2-4: Optimize the order of activities to minimize travel time.
 */
public class OptimizeRouteRequest {
    
    private String itineraryId;
    private Integer dayNumber;
    private List<String> nodeIds; // IDs of nodes to optimize
    private Coordinates startLocation; // Optional: fixed start point
    private Coordinates endLocation; // Optional: fixed end point
    
    public OptimizeRouteRequest() {
    }
    
    public OptimizeRouteRequest(String itineraryId, Integer dayNumber, List<String> nodeIds) {
        this.itineraryId = itineraryId;
        this.dayNumber = dayNumber;
        this.nodeIds = nodeIds;
    }
    
    // Getters and setters
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public Integer getDayNumber() {
        return dayNumber;
    }
    
    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }
    
    public List<String> getNodeIds() {
        return nodeIds;
    }
    
    public void setNodeIds(List<String> nodeIds) {
        this.nodeIds = nodeIds;
    }
    
    public Coordinates getStartLocation() {
        return startLocation;
    }
    
    public void setStartLocation(Coordinates startLocation) {
        this.startLocation = startLocation;
    }
    
    public Coordinates getEndLocation() {
        return endLocation;
    }
    
    public void setEndLocation(Coordinates endLocation) {
        this.endLocation = endLocation;
    }
}
