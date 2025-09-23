package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ChangePreferences DTO for user preferences in change operations.
 */
public class ChangePreferences {
    
    @JsonProperty("userFirst")
    private Boolean userFirst = true;
    
    @JsonProperty("autoApply")
    private Boolean autoApply = false;
    
    @JsonProperty("respectLocks")
    private Boolean respectLocks = true;
    
    public ChangePreferences() {}
    
    public ChangePreferences(Boolean userFirst, Boolean autoApply, Boolean respectLocks) {
        this.userFirst = userFirst;
        this.autoApply = autoApply;
        this.respectLocks = respectLocks;
    }
    
    // Getters and Setters
    public Boolean getUserFirst() {
        return userFirst;
    }
    
    public void setUserFirst(Boolean userFirst) {
        this.userFirst = userFirst;
    }
    
    public Boolean getAutoApply() {
        return autoApply;
    }
    
    public void setAutoApply(Boolean autoApply) {
        this.autoApply = autoApply;
    }
    
    public Boolean getRespectLocks() {
        return respectLocks;
    }
    
    public void setRespectLocks(Boolean respectLocks) {
        this.respectLocks = respectLocks;
    }
    
    @Override
    public String toString() {
        return "ChangePreferences{" +
                "userFirst=" + userFirst +
                ", autoApply=" + autoApply +
                ", respectLocks=" + respectLocks +
                '}';
    }
}
