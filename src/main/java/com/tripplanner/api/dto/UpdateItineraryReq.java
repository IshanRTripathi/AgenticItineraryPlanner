package com.tripplanner.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an itinerary.
 */
public record UpdateItineraryReq(
        @NotBlank(message = "Summary is required")
        @Size(max = 1000, message = "Summary must not exceed 1000 characters")
        String summary,
        
        @NotNull(message = "Interests list cannot be null")
        java.util.List<String> interests,
        
        @NotNull(message = "Constraints list cannot be null")
        java.util.List<String> constraints
) {}
