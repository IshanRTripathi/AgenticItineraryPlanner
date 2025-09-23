package com.tripplanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for revising an itinerary.
 */
public record ReviseRequest(
        @NotBlank(message = "Instructions are required")
        @Size(max = 2000, message = "Instructions must not exceed 2000 characters")
        String instructions,
        
        Integer focusDay
) {}
