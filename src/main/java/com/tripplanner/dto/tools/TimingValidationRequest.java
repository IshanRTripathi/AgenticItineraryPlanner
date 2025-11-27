package com.tripplanner.dto.tools;

import com.tripplanner.dto.NormalizedNode;
import java.util.List;

/**
 * Request DTO for timing validation tool.
 * P2-2: Validate if proposed schedule is physically feasible.
 */
public class TimingValidationRequest {
    
    private String itineraryId;
    private Integer dayNumber;
    private List<NormalizedNode> proposedSchedule;
    
    public TimingValidationRequest() {
    }
    
    public TimingValidationRequest(String itineraryId, Integer dayNumber) {
        this.itineraryId = itineraryId;
        this.dayNumber = dayNumber;
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
    
    public List<NormalizedNode> getProposedSchedule() {
        return proposedSchedule;
    }
    
    public void setProposedSchedule(List<NormalizedNode> proposedSchedule) {
        this.proposedSchedule = proposedSchedule;
    }
}
