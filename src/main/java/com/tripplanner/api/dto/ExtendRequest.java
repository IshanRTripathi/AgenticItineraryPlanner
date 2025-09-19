package com.tripplanner.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for extending an itinerary.
 */
public record ExtendRequest(
        @NotNull(message = "Additional days is required")
        @Positive(message = "Additional days must be positive")
        Integer additionalDays,
        
        String reason
) {}
