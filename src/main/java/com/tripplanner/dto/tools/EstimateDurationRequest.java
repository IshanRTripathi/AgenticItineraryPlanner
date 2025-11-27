package com.tripplanner.dto.tools;

public class EstimateDurationRequest {
    private String activityType;
    private String activityName;
    private Integer partySize;
    
    public EstimateDurationRequest() {
    }
    
    public String getActivityType() {
        return activityType;
    }
    
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }
    
    public String getActivityName() {
        return activityName;
    }
    
    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }
    
    public Integer getPartySize() {
        return partySize;
    }
    
    public void setPartySize(Integer partySize) {
        this.partySize = partySize;
    }
}
