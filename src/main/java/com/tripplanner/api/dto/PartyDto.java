package com.tripplanner.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for party information in travel requests.
 */
@Data
@Builder
public class PartyDto {
    private int adults;
    @Builder.Default
    private int children = 0;
    @Builder.Default
    private int infants = 0;
    @Builder.Default
    private int rooms = 1;
    
    public PartyDto() {
        // Default constructor for Lombok
    }
    
    public PartyDto(int adults, int children, int infants, int rooms) {
        this.adults = adults;
        this.children = Math.max(0, children);
        this.infants = Math.max(0, infants);
        this.rooms = Math.max(1, rooms);
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
        
        return PartyDto.builder()
                .adults(entity.getAdults())
                .children(entity.getChildren())
                .infants(entity.getInfants())
                .rooms(entity.getRooms())
                .build();
    }
}
