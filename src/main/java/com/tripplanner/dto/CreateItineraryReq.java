package com.tripplanner.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a new itinerary.
 */
@Data
@Builder
public class CreateItineraryReq {
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private PartyDto party;
    private String budgetTier;
    @Builder.Default
    private List<String> interests = List.of();
    @Builder.Default
    private List<String> constraints = List.of();
    @Builder.Default
    private String language = "en";
    
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

