package com.tripplanner.dto.tools;

public class GetNodesByTypeRequest {
    private String itineraryId;
    private String nodeType;
    private Integer dayNumber;
    
    public GetNodesByTypeRequest() {
    }
    
    public GetNodesByTypeRequest(String itineraryId, String nodeType) {
        this.itineraryId = itineraryId;
        this.nodeType = nodeType;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public String getNodeType() {
        return nodeType;
    }
    
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }
    
    public Integer getDayNumber() {
        return dayNumber;
    }
    
    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }
}
