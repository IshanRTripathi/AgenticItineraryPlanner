package com.tripplanner.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a new itinerary.
 */
public record CreateItineraryReq(
        @NotBlank(message = "Destination is required")
        @Size(max = 200, message = "Destination must not exceed 200 characters")
        String destination,
        
        @NotNull(message = "Start date is required")
        @Future(message = "Start date must be in the future")
        LocalDate startDate,
        
        @NotNull(message = "End date is required")
        @Future(message = "End date must be in the future")
        LocalDate endDate,
        
        @Valid
        @NotNull(message = "Party information is required")
        PartyDto party,
        
        @NotBlank(message = "Budget tier is required")
        @Pattern(regexp = "economy|mid-range|luxury", message = "Budget tier must be one of: economy, mid-range, luxury")
        String budgetTier,
        
        @Size(max = 20, message = "Maximum 20 interests allowed")
        List<@NotBlank @Size(max = 50) String> interests,
        
        @Size(max = 10, message = "Maximum 10 constraints allowed")
        List<@NotBlank @Size(max = 100) String> constraints,
        
        @NotBlank(message = "Language is required")
        @Pattern(regexp = "en|es|fr|de|it|pt|ja|ko|zh", message = "Unsupported language")
        String language
) {
    
    public CreateItineraryReq {
        // Validation: end date must be after start date
        if (startDate != null && endDate != null && !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        
        // Validation: trip duration should be reasonable (max 30 days for MVP)
        if (startDate != null && endDate != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            if (days > 30) {
                throw new IllegalArgumentException("Trip duration cannot exceed 30 days");
            }
            if (days < 1) {
                throw new IllegalArgumentException("Trip duration must be at least 1 day");
            }
        }
        
        // Set defaults for null lists
        if (interests == null) {
            interests = List.of();
        }
        if (constraints == null) {
            constraints = List.of();
        }
    }
    
    /**
     * Calculate trip duration in days.
     */
    public int getDurationDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }
    
    /**
     * Get total party size.
     */
    public int getTotalPartySize() {
        return party != null ? party.getTotalGuests() : 0;
    }
}
