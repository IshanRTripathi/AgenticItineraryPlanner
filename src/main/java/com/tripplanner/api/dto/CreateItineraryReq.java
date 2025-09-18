package com.tripplanner.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a new itinerary.
 */
public record CreateItineraryReq(
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        PartyDto party,
        String budgetTier,
        List<String> interests,
        List<String> constraints,
        String language
) {
    
    public CreateItineraryReq {
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

