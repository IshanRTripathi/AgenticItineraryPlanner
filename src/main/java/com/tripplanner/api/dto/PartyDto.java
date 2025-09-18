package com.tripplanner.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * DTO for party information in travel requests.
 */
public record PartyDto(
        int adults,
        int children,
        int infants,
        int rooms
) {
    
    public PartyDto {
        // Set defaults for zero values
        if (children < 0) children = 0;
        if (infants < 0) infants = 0;
        if (rooms < 1) rooms = 1;
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
