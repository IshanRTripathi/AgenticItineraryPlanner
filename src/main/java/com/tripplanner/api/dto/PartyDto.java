package com.tripplanner.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * DTO for party information in travel requests.
 */
public record PartyDto(
        @Min(value = 1, message = "At least 1 adult is required")
        @Max(value = 20, message = "Maximum 20 adults allowed")
        int adults,
        
        @Min(value = 0, message = "Children count cannot be negative")
        @Max(value = 15, message = "Maximum 15 children allowed")
        int children,
        
        @Min(value = 0, message = "Infants count cannot be negative")
        @Max(value = 10, message = "Maximum 10 infants allowed")
        int infants,
        
        @Min(value = 1, message = "At least 1 room is required")
        @Max(value = 10, message = "Maximum 10 rooms allowed")
        int rooms
) {
    
    public PartyDto {
        // Set defaults for zero values
        if (children < 0) children = 0;
        if (infants < 0) infants = 0;
        if (rooms < 1) rooms = 1;
        
        // Validation: total guests should be reasonable
        int totalGuests = adults + children + infants;
        if (totalGuests > 25) {
            throw new IllegalArgumentException("Total party size cannot exceed 25 guests");
        }
        
        // Validation: rooms should accommodate the party
        if (rooms > totalGuests) {
            throw new IllegalArgumentException("Number of rooms cannot exceed number of guests");
        }
    }
    
    /**
     * Get total number of guests.
     */
    public int getTotalGuests() {
        return adults + children + infants;
    }
    
    /**
     * Get average guests per room.
     */
    public double getAverageGuestsPerRoom() {
        return (double) getTotalGuests() / rooms;
    }
    
    /**
     * Check if party includes children.
     */
    public boolean hasChildren() {
        return children > 0 || infants > 0;
    }
    
    /**
     * Get paying guests count (adults + children, excluding infants).
     */
    public int getPayingGuests() {
        return adults + children;
    }
    
    /**
     * Create DTO from entity.
     */
    public static PartyDto fromEntity(com.tripplanner.data.entity.Itinerary.Party entity) {
        if (entity == null) {
            return null;
        }
        
        return new PartyDto(
                entity.getAdults(),
                entity.getChildren(),
                entity.getInfants(),
                entity.getRooms()
        );
    }
}
