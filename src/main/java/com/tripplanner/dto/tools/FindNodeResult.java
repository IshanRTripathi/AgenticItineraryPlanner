package com.tripplanner.dto.tools;

import com.tripplanner.dto.NormalizedNode;

public class FindNodeResult {
    private boolean success;
    private NormalizedNode node;
    private Integer dayNumber;
    private String error;
    
    public FindNodeResult() {
    }
    
    public static FindNodeResult error(String error) {
        FindNodeResult result = new FindNodeResult();
        result.success = false;
        result.error = error;
        return result;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public NormalizedNode getNode() {
        return node;
    }
    
    public void setNode(NormalizedNode node) {
        this.node = node;
    }
    
    public Integer getDayNumber() {
        return dayNumber;
    }
    
    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
