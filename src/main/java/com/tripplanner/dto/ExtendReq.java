package com.tripplanner.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request DTO for extending an itinerary.
 */
public record ExtendReq(
        @Min(value = 1, message = "Must extend by at least 1 day")
        @Max(value = 30, message = "Cannot extend by more than 30 days")
        int days
) {
    
    /**
     * Check if extension is reasonable (less than a week).
     */
    public boolean isReasonableExtension() {
        return days <= 7;
    }
    
    /**
     * Check if extension is major (more than a week).
     */
    public boolean isMajorExtension() {
        return days > 7;
    }
}

