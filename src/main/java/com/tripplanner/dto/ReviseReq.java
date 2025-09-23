package com.tripplanner.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for revising an itinerary.
 */
public record ReviseReq(
        @NotBlank(message = "Revision instructions are required")
        @Size(max = 2000, message = "Instructions must not exceed 2000 characters")
        String instructions,
        
        @Min(value = 1, message = "Focus day must be at least 1")
        @Max(value = 30, message = "Focus day must not exceed 30")
        Integer focusDay
) {
    
    /**
     * Check if revision is focused on a specific day.
     */
    public boolean hasFocusDay() {
        return focusDay != null;
    }
    
    /**
     * Get instructions with focus day context if applicable.
     */
    public String getContextualInstructions() {
        if (focusDay != null) {
            return String.format("For day %d: %s", focusDay, instructions);
        }
        return instructions;
    }
}

