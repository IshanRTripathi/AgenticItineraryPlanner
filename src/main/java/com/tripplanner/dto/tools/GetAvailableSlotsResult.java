package com.tripplanner.dto.tools;

import java.util.ArrayList;
import java.util.List;

public class GetAvailableSlotsResult {
    private boolean success;
    private List<TimeSlot> availableSlots;
    private String error;
    
    public GetAvailableSlotsResult() {
        this.availableSlots = new ArrayList<>();
    }
    
    public static GetAvailableSlotsResult error(String error) {
        GetAvailableSlotsResult result = new GetAvailableSlotsResult();
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
    
    public List<TimeSlot> getAvailableSlots() {
        return availableSlots;
    }
    
    public void setAvailableSlots(List<TimeSlot> availableSlots) {
        this.availableSlots = availableSlots;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public static class TimeSlot {
        private Long startTime;
        private Long endTime;
        private int durationMinutes;
        
        public TimeSlot() {
        }
        
        public TimeSlot(Long startTime, Long endTime, int durationMinutes) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationMinutes = durationMinutes;
        }
        
        public Long getStartTime() {
            return startTime;
        }
        
        public void setStartTime(Long startTime) {
            this.startTime = startTime;
        }
        
        public Long getEndTime() {
            return endTime;
        }
        
        public void setEndTime(Long endTime) {
            this.endTime = endTime;
        }
        
        public int getDurationMinutes() {
            return durationMinutes;
        }
        
        public void setDurationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
        }
    }
}
